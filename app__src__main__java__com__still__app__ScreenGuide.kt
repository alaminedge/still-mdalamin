package com.still.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class QA(val q: String, val a: String)
private data class Section(val title: String, val items: List<QA>)

private val GUIDE = listOf(
    Section(
        "Getting started", listOf(
            QA(
                "What is STILL?",
                "STILL helps you use your phone on purpose. You choose which apps to block, when to block them, and how long you can use them each day. It also shows how much screen time you have used today and can replace your home screen with a quiet, minimal one."
            ),
            QA(
                "What do I need to turn on first?",
                "Two things, both on the Today tab under \"Finish setup\":\n\n1. Blocking engine: opens Android Accessibility settings. Find STILL in the list and switch it on. This is what lets STILL stop a blocked app.\n\n2. Usage access: opens Android's usage access list. Find STILL and allow it. This is what lets STILL measure screen time and daily limits.\n\nWithout the first, nothing gets blocked. Without the second, screen time and limits cannot work."
            ),
            QA(
                "The Accessibility switch is greyed out or says \"Restricted setting\".",
                "This is an Android 13+ safety rule for apps installed from outside the Play Store. Fix: open Android Settings > Apps > STILL, tap the three dots at the top right, choose \"Allow restricted settings\", then go back and switch STILL on in Accessibility."
            ),
            QA(
                "Why does STILL need Accessibility? Is it reading my screen?",
                "STILL only uses it to notice which app just came to the front, so it can check your rules. It does not read, store or send what is on your screen, your messages or your passwords. You can read the same promise in the description on Android's Accessibility page."
            )
        )
    ),
    Section(
        "Today tab", listOf(
            QA(
                "What does the big ring mean?",
                "It is your daily phone limit. Tap the card to set a limit for the whole phone (1h to 8h). The ring then shows how much time is left today and slowly empties as you use the phone. It turns amber when under a quarter is left and red when you go over.\n\nImportant: for now this limit is a goal. It does not block the phone by itself. Blocking happens through the per-app rules on the Focus tab."
            ),
            QA(
                "How is screen time counted? Why is it different from Digital Wellbeing?",
                "STILL adds up the time each app was in the foreground since midnight, using Android's own usage records. Small differences from other apps are normal because each tool counts brief switches and screen-off moments slightly differently. The count resets at midnight."
            ),
            QA(
                "What do the chips and the lock icon mean?",
                "The chip at the top shows the state at a glance, for example \"Focus on\", \"Hardcore 4d 23h\" or \"2 on home\". A small lock next to an app in the usage list means that app is blocked right now."
            ),
            QA(
                "Why is the usage list empty?",
                "Either usage access is not allowed yet, or you have not used any app for more than one minute today. Only apps used for over a minute are listed."
            )
        )
    ),
    Section(
        "Focus and blocking", listOf(
            QA(
                "How do I block an app?",
                "Open the Focus tab, tap the app, and choose one of three options:\n\n• Not blocked: no rule.\n• Focus hours: blocked only during your focus schedules.\n• Always blocked: blocked all the time.\n\nYou can also add a daily time limit for the app on top of this."
            ),
            QA(
                "How do focus hours work?",
                "Add a schedule such as Study (9 AM to 1 PM) or Sleep (10 PM to 6 AM). Pick the days and the start and end times. Overnight schedules work, so Sleep carries on past midnight. Apps set to \"Focus hours\" are blocked only while a schedule is running. You can switch a schedule off without deleting it."
            ),
            QA(
                "When does a daily app limit reset?",
                "At midnight. If you reach the limit, the app is blocked for the rest of the day."
            )
        ) + listOf(
            QA(
                "What happens when I open a blocked app?",
                "STILL sends you home and shows a calm \"Not now.\" screen with the reason. It checks when you open the app, and again every 20 seconds while you stay in it, so staying inside an app after your limit ends does not work."
            ),
            QA(
                "A blocked app opened anyway. Why?",
                "Most often the Blocking engine is off. After a phone update or restart, check the Today tab: if \"Finish setup\" is showing, turn Accessibility on again. Also see \"Blocking stops after a while\" under Troubleshooting."
            )
        )
    ),
    Section(
        "Home tab (minimal home)", listOf(
            QA(
                "What is the minimal home?",
                "STILL can be your phone's home screen. It shows only the apps you pin, in a calm layout, with everything else one search away. Open the Home tab, tap \"Set as home app\", and choose STILL. Use \"Preview\" to look first."
            ),
            QA(
                "What does Hidden mean? Did the app get deleted?",
                "No. A hidden app stays installed and keeps working. It just disappears from STILL's home and app list so it stops tempting you. It still shows in Android Settings > Apps."
            ),
            QA(
                "How do I go back to my old home screen?",
                "Open Android Settings > Apps > Default apps > Home app, and choose your old launcher. If Hardcore Mode is on, STILL keeps its own pages closed until it ends."
            )
        )
    ),
    Section(
        "Hardcore Mode", listOf(
            QA(
                "What is Hardcore Mode?",
                "A promise you cannot break easily. You pick 1, 3, 7 or 30 days. During that time you cannot loosen your rules, so you cannot talk yourself out of them on a weak moment."
            ),
            QA(
                "What exactly gets locked?",
                "• You cannot turn off a block or lower it (for example Always to Focus hours).\n• You cannot remove or raise a daily limit, including the phone limit.\n• You cannot delete or edit focus schedules.\n• Hidden apps stay hidden.\n• STILL's own pages in Android Settings are closed, so you cannot switch it off or uninstall it from there.\n\nYou can still make rules stricter at any time: add blocks, lower limits, add apps."
            ),
            QA(
                "Can I cancel it early?",
                "No, and that is the point. It ends on the date shown in the More tab. Start with 1 day to get a feel for it before choosing longer."
            ),
            QA(
                "What is Uninstall protection?",
                "It turns on Android's Device Admin for STILL, which makes the app harder to remove during Hardcore Mode. Switch it on in the More tab before you start."
            ),
            QA(
                "Is Hardcore completely unbreakable?",
                "No. It is strong friction, not a vault. A person who really wants to can still restart in safe mode or factory-reset the phone. STILL is built to stop impulse, not to fight you. Use it only when you are sure."
            )
        )
    ),
    Section(
        "Emergency unlock", listOf(
            QA(
                "What is it for?",
                "A real need, such as a call app or a maps app you blocked. It is deliberately slow so impulse has time to pass."
            ),
            QA(
                "How does it work?",
                "On the block screen tap \"Emergency unlock\", wait for the timer, then unlock that app for 5 minutes. After 5 minutes the rule applies again.\n\nNormal: 3 per day, 60 second wait.\nHardcore: 1 per day, 3 minute wait.\n\nThe count resets every day. The More tab shows how many you have left."
            )
        )
    ),
    Section(
        "Background control", listOf(
            QA(
                "What does it do?",
                "You pick apps you want quieter. STILL then opens each app's Android battery and notification pages so you can restrict them in one tap. Android decides how far that goes: essential background work may still run, and only you or the system can force-stop an app."
            )
        )
    ),
    Section(
        "Privacy and safety", listOf(
            QA(
                "Does STILL send my data anywhere?",
                "No. STILL does not ask for internet permission, so it cannot send anything off your phone. It has no account, no sign-up and no ads. Your rules and settings are saved only on your device, and Android backup is turned off for the app."
            ),
            QA(
                "What can STILL see?",
                "Which app is on screen (to apply your rules), how long each app was used today (for screen time), and the list of apps installed (to show them to you). Nothing else."
            ),
            QA(
                "How do I remove STILL?",
                "Without Hardcore Mode: turn off Device Admin (Android Settings > Security > Device admin apps), then uninstall as usual. If Hardcore Mode is on, you must wait until it ends."
            )
        )
    ),
    Section(
        "Troubleshooting", listOf(
            QA(
                "Blocking stops after a while.",
                "Some phone makers (Xiaomi, Samsung, Oppo, Vivo, Huawei and others) stop background apps to save battery. Open Android Settings > Apps > STILL > Battery and choose \"Unrestricted\" (or \"No restrictions\"). On Xiaomi also allow Autostart, and lock STILL in the recent apps screen."
            ),
            QA(
                "The app list is slow or empty the first time.",
                "STILL loads your apps in the background as soon as it starts. On the very first launch with many apps it can take a moment, and you will see grey placeholder rows. After that it opens instantly."
            ),
            QA(
                "I blocked something I really need.",
                "Use Emergency unlock for a quick need. To change the rule for good, go to the Focus tab and set the app to \"Not blocked\". If Hardcore Mode is on, the rule stays until it ends."
            ),
            QA(
                "Accessibility turned itself off.",
                "Android can switch services off after an update or when it is low on memory. Turn it on again from the Today tab. Setting STILL's battery to Unrestricted helps stop this."
            )
        )
    )
)

@Composable
fun ScreenGuide(onBack: () -> Unit) {
    BackHandler { onBack() }
    var open by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { TextButton(onClick = onBack) { Text("‹  Back to More") } }
        item { Header("Guide", "Everything about STILL, in plain words.") }

        GUIDE.forEach { sec ->
            item(key = "s-" + sec.title) { SectionLabel(sec.title) }
            item(key = "p-" + sec.title) {
                Panel {
                    sec.items.forEachIndexed { i, qa ->
                        val isOpen = open == qa.q
                        if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Column(Modifier.fillMaxWidth().pressClickable { open = if (isOpen) null else qa.q }.padding(vertical = 12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    qa.q,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    if (isOpen) "–" else "+",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            AnimatedVisibility(visible = isOpen) {
                                Text(
                                    qa.a,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(6.dp)); AboutCard() }
    }
}

@Composable
fun AboutCard() {
    val c = LocalContext.current
    val ver = remember {
        try { c.packageManager.getPackageInfo(c.packageName, 0).versionName ?: "1.0" } catch (e: Exception) { "1.0" }
    }
    Panel {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            StillLogo(64.dp, animate = false)
            Spacer(Modifier.height(10.dp))
            Text("STILL", style = MaterialTheme.typography.titleLarge.copy(letterSpacing = 6.sp))
            Text(
                "Use your phone. Don't let it use you.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(16.dp))
            Text("DEVELOPED BY", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text("Md. Al Amin", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Text(
                "Barguna, Bangladesh",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(14.dp))
            Text(
                "Version $ver",
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
