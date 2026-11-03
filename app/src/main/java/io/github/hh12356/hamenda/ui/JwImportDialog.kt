package io.github.hh12356.hamenda.ui

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.doOnLayout
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.hh12356.hamenda.data.CourseBlock
import io.github.hh12356.hamenda.data.JW_LOGIN_URL
import io.github.hh12356.hamenda.data.parseZfTimetable
import io.github.hh12356.hamenda.data.zfTerm
import io.github.hh12356.hamenda.data.zfTermBody

/**
 * 内嵌教务登录页：账号密码只在教务页面里输入，应用不碰也不存；
 * 登录跳回首页后再用同一会话取课表。
 */
// shortcut: 只认 jwgl.whu.edu.cn 和正方新版首页跳转，换学校或系统改版要改这里。
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun JwImportDialog(
    semesterStart: String,
    onDismiss: () -> Unit,
    onResult: (List<CourseBlock>) -> Unit
) {
    val context = LocalContext.current
    val (xnm, xqm) = remember { zfTerm(semesterStart) }
    var error by remember { mutableStateOf<String?>(null) }
    // 卡住时至少能看出停在哪一步。
    var pageUrl by remember { mutableStateOf("") }
    var debug by remember { mutableStateOf("") }
    val termLabel = remember {
        "$xnm-${xnm.toInt() + 1} 学年 · " + if (xqm == "3") "第一学期" else "第二学期"
    }
    val web = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            // 学校页面是手机版布局，不认 viewport meta 的话整块内容会被挤没。
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            // 统一认证的验证码/二维码跨域加载，三方 Cookie 关掉会一直转圈。
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            // 带 "wv" 标记时部分统一认证页面会走精简流程并卡住，伪装成普通 Chrome。
            settings.userAgentString = settings.userAgentString
                .replace("; wv", "").replace("Version/4.0 ", "")
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            // 不设 WebChromeClient 时页面里的 alert/confirm 会被直接吞掉。
            // 顺手把脚本报错打到 logcat：页面白屏时只能靠它定位。
            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(m: ConsoleMessage): Boolean {
                    Log.d("JwWeb", "${m.messageLevel()} ${m.message()} @${m.sourceId()}:${m.lineNumber()}")
                    return true
                }
            }
        }
    }

    DisposableEffect(Unit) {
        val tried = mutableSetOf<String>()
        var lastUrl = ""
        val bridge = object {
            // 手机上取不到 logcat，取课表的过程实测只能靠这行显示出来。
            @JavascriptInterface
            fun onDebug(text: String) {
                Handler(Looper.getMainLooper()).post { debug = text }
            }

            @JavascriptInterface
            fun onTimetable(json: String) {
                Handler(Looper.getMainLooper()).post {
                    val blocks = parseZfTimetable(json)
                    when {
                        blocks == null -> if (lastUrl.contains("xtgl/index")) {
                            error = "课表数据解析失败，可能登录已过期"
                        }
                        blocks.isEmpty() -> if (lastUrl.contains("xtgl/index")) {
                            error = "该学期没有查到课程"
                        }
                        else -> onResult(blocks)
                    }
                }
            }
        }
        web.addJavascriptInterface(bridge, "Hamenda")
        web.webViewClient = object : WebViewClient() {
            // 登录成功后教务会把我们送回 jwgl 的首页，这时用同一会话取课表。
            // 认证中途的 /sso/ 页面也顺手试一次，失败就等下一次跳转。
            override fun onPageFinished(view: WebView, url: String) {
                lastUrl = url
                pageUrl = url.substringAfter("://")
                if (url.contains("login_slogin") || !tried.add(url)) return
                if (url.startsWith("https://jwgl.whu.edu.cn/")) {
                    view.evaluateJavascript(fetchJs(xnm, xqm), null)
                }
                // 学校移动版页面按 100vh 排版，而 WebView 里 100vh 会算成 0，
                // 整块内容被 overflow 裁掉显示成白屏；改成按内容撑开。
                view.evaluateJavascript(FIX_VH_JS, null)
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                err: WebResourceError
            ) {
                if (request.isForMainFrame) {
                    Log.d("JwWeb", "load failed ${request.url}: ${err.description}")
                }
            }
        }
        web.loadUrl(JW_LOGIN_URL)
        onDispose {
            web.removeJavascriptInterface("Hamenda")
            web.destroy()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(0.94f).fillMaxHeight(0.88f)
        ) {
            Column {
                Row(
                    Modifier.fillMaxWidth().padding(start = 20.dp, top = 8.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("登录教务系统", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "导入 $termLabel",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (pageUrl.isNotEmpty()) {
                            Text(
                                pageUrl,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (debug.isNotEmpty()) {
                            Text(
                                debug,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    TextButton(onClick = onDismiss) { Text("取消") }
                }
                error?.let {
                    Text(
                        it,
                        Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                AndroidView({ web }, Modifier.weight(1f).fillMaxWidth())
            }
        }
    }
}

// 学校页面用 100vh 排版，而 WebView 里 100vh 会算成 0：容器高度变 0、内容被裁成白屏，
// 弹窗也因为居中在 0 高度盒子里被顶到最上面。这里把塌掉的容器改回按内容撑开。
private const val FIX_VH_JS = """(function(){
  if (window.__hamendaFix) return;
  window.__hamendaFix = true;
  var s = document.createElement('style');
  s.textContent = '.mobile-page-center-box{height:auto!important}' +
    '.mobile-page-content-box{max-height:none!important;overflow:visible!important}';
  document.head.appendChild(s);
  function fix() {
    var els = document.querySelectorAll('div,section,form,main');
    for (var i = 0; i < els.length; i++) {
      var el = els[i], cs = getComputedStyle(el);
      if (cs.height === '0px' && el.children.length &&
          (cs.overflow === 'hidden' || cs.overflowY === 'hidden' || cs.display === 'flex')) {
        el.style.setProperty('height', 'auto', 'important');
      }
    }
  }
  var pending = false;
  function schedule() {
    if (pending) return;
    pending = true;
    requestAnimationFrame(function () { pending = false; fix(); });
  }
  fix();
  // 多因素、信任设备这些弹窗是登录后才插进 DOM 的，得盯着变化再修一次。
  new MutationObserver(schedule).observe(document.body, { childList: true, subtree: true });
})()"""

// 学年/学期的代码各校不一样（正方常用 3/12，也有 1/2），所以先读课表页自己的默认值，
// 再按候选依次试，取第一个真的带回课程的响应。
private fun fetchJs(xnm: String, xqm: String): String {
    val candidates = listOf(xqm, "3", "12", "1", "2").distinct()
        .joinToString(",") { "'xnm=$xnm&xqm=$it'" }
    return """
    (async function () {
      var url = '/kbcx/xskbcx_cxXsgrkb.html?gnmkdm=N2151';
      var headers = {
        'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
        'X-Requested-With': 'XMLHttpRequest'
      };
      function post(body) {
        return fetch(url, { method: 'POST', credentials: 'include', headers: headers, body: body })
          .then(function (r) { return r.text() });
      }
      var candidates = [];
      try {
        var html = await fetch(url, { credentials: 'include' }).then(function (r) { return r.text() });
        var doc = new DOMParser().parseFromString(html, 'text/html');
        var y = doc.querySelector('#xnm'), q = doc.querySelector('#xqm');
        if (y && q && y.value && q.value) candidates.push('xnm=' + y.value + '&xqm=' + q.value);
      } catch (e) { }
      [$candidates].forEach(function (b) { if (candidates.indexOf(b) < 0) candidates.push(b) });
      var last = '', log = '';
      for (var i = 0; i < candidates.length; i++) {
        last = await post(candidates[i]);
        log += candidates[i] + ' len=' + last.length + '; ';
        try {
          var json = JSON.parse(last);
          if (json.kbList && json.kbList.length) break;
        } catch (e) { }
      }
      Hamenda.onDebug(log);
      Hamenda.onTimetable(last);
    })();
""".trimIndent()
}
