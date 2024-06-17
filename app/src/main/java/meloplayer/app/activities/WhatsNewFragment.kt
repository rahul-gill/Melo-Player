package meloplayer.app.activities

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import meloplayer.app.BuildConfig
import meloplayer.app.databinding.FragmentWhatsNewBinding
import meloplayer.app.extensions.accentColor
import meloplayer.app.util.PreferenceUtil.lastVersion
import meloplayer.appthemehelper.util.ATHUtil.isWindowBackgroundDark
import meloplayer.appthemehelper.util.ColorUtil.isColorLight
import meloplayer.appthemehelper.util.ColorUtil.lightenColor
import meloplayer.appthemehelper.util.MaterialValueHelper.getPrimaryTextColor
import java.nio.charset.StandardCharsets
import java.util.Locale

class WhatsNewFragment : BottomSheetDialogFragment() {
    private var _binding: FragmentWhatsNewBinding? = null
    val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWhatsNewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            val buf = StringBuilder()
            val stream = requireContext().assets.open("retro-changelog.html")
            stream.reader(StandardCharsets.UTF_8).buffered().use { br ->
                var str: String?
                while (br.readLine().also { str = it } != null) {
                    buf.append(str)
                }
            }

            // Inject color values for WebView body background and links
            val isDark = isWindowBackgroundDark(requireContext())
            val accentColor = accentColor()
            binding.webView.setBackgroundColor(0)
            val contentColor = colorToCSS(Color.parseColor(if (isDark) "#ffffff" else "#000000"))
            val textColor = colorToCSS(Color.parseColor(if (isDark) "#60FFFFFF" else "#80000000"))
            val accentColorString = colorToCSS(accentColor())
            val cardBackgroundColor =
                colorToCSS(Color.parseColor(if (isDark) "#353535" else "#ffffff"))
            val accentTextColor = colorToCSS(
                getPrimaryTextColor(
                    requireContext(), isColorLight(accentColor)
                )
            )
            val changeLog = buf.toString()
                .replace(
                    "{style-placeholder}",
                    "body { color: $contentColor; } li {color: $textColor;} h3 {color: $accentColorString;} .highlight {color: $accentColorString;} .tag {background-color: $accentColorString; color: $accentTextColor; } div{background-color: $cardBackgroundColor;}"
                )
                .replace("{link-color}", colorToCSS(accentColor()))
                .replace(
                    "{link-color-active}",
                    colorToCSS(
                        lightenColor(accentColor())
                    )
                )
            binding.webView.loadData(changeLog, "text/html", "UTF-8")
            binding.webView.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val url = request?.url ?: return false
                    //you can do checks here e.g. url.host equals to target one
                    startActivity(Intent(Intent.ACTION_VIEW, url))
                    return true
                }
            }
        } catch (e: Throwable) {
            binding.webView.loadData(
                "<h1>Unable to load</h1><p>" + e.localizedMessage + "</p>", "text/html", "UTF-8"
            )
        }
        setChangelogRead(requireContext())
        binding.tgFab.setOnClickListener {
            //TODO: openUrl(Constants.TELEGRAM_CHANGE_LOG)
        }
        binding.tgFab.accentColor()
        binding.tgFab.shrink()
        binding.container.setOnScrollChangeListener { _: NestedScrollView?, _: Int, scrollY: Int, _: Int, oldScrollY: Int ->
            val dy = scrollY - oldScrollY
            if (dy > 0) {
                binding.tgFab.shrink()
            } else if (dy < 0) {
                binding.tgFab.extend()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    companion object {

        const val TAG = "WhatsNewFragment"
        private fun colorToCSS(color: Int): String {
            return String.format(
                Locale.getDefault(),
                "rgba(%d, %d, %d, %d)",
                Color.red(color),
                Color.green(color),
                Color.blue(color),
                Color.alpha(color)
            ) // on API 29, WebView doesn't load with hex colors
        }

        private fun setChangelogRead(context: Context) {
            try {
                val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                val currentVersion = PackageInfoCompat.getLongVersionCode(pInfo)
                lastVersion = currentVersion
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }
        }

        fun showChangeLog(activity: FragmentActivity) {
            val pInfo = activity.packageManager.getPackageInfo(activity.packageName, 0)
            val currentVersion = PackageInfoCompat.getLongVersionCode(pInfo)
            if (currentVersion > lastVersion && !BuildConfig.DEBUG) {
                val changelogBottomSheet = WhatsNewFragment()
                changelogBottomSheet.show(activity.supportFragmentManager, TAG)
            }
        }
    }
}