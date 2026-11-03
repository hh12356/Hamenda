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
    val termLabel = remember {
        "$xnm-${xnm.toInt() + 1} 学年 · " + if (xqm == "3") "第一学期" else "第二学期"
    }
    val web = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
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
                    view.evaluateJavascript(fetchJs(zfTermBody(xnm, xqm)), null)
                }
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

private fun fetchJs(body: String) = """
    fetch('/kbcx/xskbcx_cxXsgrkb.html?gnmkdm=N2151', {
      method: 'POST',
      credentials: 'include',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
        'X-Requested-With': 'XMLHttpRequest'
      },
      body: '$body'
    }).then(function (r) { return r.text() })
      .then(function (t) { Hamenda.onTimetable(t) })
      .catch(function () { Hamenda.onTimetable('') });
""".trimIndent()
