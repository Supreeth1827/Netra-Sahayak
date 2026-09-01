# Setup — start here

Getting **Netra Sahayak** running takes about 20 minutes, most of which is Android Studio
downloading things. You do **not** need the AI backend, a server, or even an internet connection
once it is installed — the app ships in demo mode and works on its own.

---

## Step 1 — Install Android Studio

Download it from **https://developer.android.com/studio** and run the installer.

Accept the default setup wizard options. It installs everything the project needs: a Java 17
runtime, the Android SDK, and an emulator. You do not need to install Java or Gradle separately.

> **Windows:** when the wizard offers to install the Android SDK, let it. The default location is
> `C:\Users\<you>\AppData\Local\Android\Sdk`.

---

## Step 2 — Get the project

Either clone it with git:

```
git clone <repository-url>
```

…or download the ZIP from GitHub (green **Code** button → **Download ZIP**) and extract it.

> **Windows tip:** put it somewhere with a **short path and no spaces**, for example `C:\dev\`.
> Very long paths can break the Android build.

---

## Step 3 — Open it

In Android Studio: **File → Open**, then select the `NetraSahayak` folder — the one containing
`settings.gradle.kts`. Do not open a folder above or below it.

Android Studio may show a yellow bar saying the **SDK location is missing** or offer to set it.
**Click the fix / accept it.** That writes a small `local.properties` file with your own SDK path.
It is not stored in the repository on purpose, because that path is different on every computer.

Now wait. The status bar at the bottom will say *"Gradle sync in progress"*. The first sync
downloads Gradle 8.7 and all the libraries — typically **5–15 minutes** depending on your
connection. This only happens once.

You are ready when the bar reads **"Gradle sync finished"** and there are no red errors.

---

## Step 4 — Create a virtual phone

**Tools → Device Manager → Create Device**

- Pick any phone, for example **Pixel 5**
- Choose a system image with **API level 24 or higher** (API 34 recommended) and download it
- Click **Finish**

If you have a real Android phone, you can skip this: enable **Developer options → USB debugging**
on the phone and plug it in with a USB cable. A real phone is faster and the camera actually works
properly.

---

## Step 5 — Run it

Select your device in the toolbar dropdown and press the green **▶ Run** button.

The first build takes a couple of minutes. After that the app opens on the device showing the
**Netra Sahayak** home screen.

---

## Step 6 — Try a screening

The app needs a retinal photo to work with. There is a sample one in the project at
`tools/sample_fundus.jpg`.

**To get it onto the emulator:** simply drag the file from your file explorer and drop it onto the
emulator window. It lands in the device's gallery.

Then in the app:

1. **START NEW SCREENING**
2. Patient ID `P001`, Age `55` → **CONTINUE**
3. **CHOOSE FROM GALLERY** → pick the fundus image
4. **ANALYZE IMAGE**

After about two seconds you get a screening result with a confidence score and a heatmap. Try the
**ORIGINAL / HEATMAP / OVERLAY** buttons, press **DONE**, then open **SCREENING HISTORY** — the
screening was saved on the device.

Also worth trying: **TAKE PHOTO** (uses the real camera), and turning off wi-fi to see that
everything except the analysis step still works.

---

## What "demo mode" means

The debug build has `USE_MOCK_API = true` in `app/build.gradle.kts`. The result you see is
**generated on the phone**, not by a real AI model — it is there so the whole interface can be
demonstrated before the Python backend exists. The grade is derived from the Patient ID, so the
same ID always gives the same result. **It carries no medical meaning whatsoever.**

To connect the real FastAPI backend later, set that flag to `false` and point `BASE_URL` at your
server. Both live in `app/build.gradle.kts`, and README §4 and §7 explain it fully.

---

## If something goes wrong

| Problem | Fix |
|---|---|
| "SDK location not found" | Android Studio → the yellow bar's fix link. Or create `local.properties` in the project root containing `sdk.dir=C\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk` |
| Gradle sync fails or times out | Check your internet connection, then **File → Sync Project with Gradle Files**. Corporate networks and VPNs often block the downloads |
| "Unsupported Java version" / toolchain errors | Use the JDK bundled with Android Studio: **Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK** → pick the one labelled *jbr-17* or *Embedded JDK* |
| Emulator won't start / is very slow | Enable virtualisation in your BIOS (Intel VT-x / AMD-V). A real phone over USB avoids this entirely |
| Very long path errors on Windows | Move the project to `C:\dev\NetraSahayak` |
| App installs but shows a blank screen | Check **Logcat** in Android Studio, filter by `netrasahayak` |

---

## Where to go next

- **`README.md`** — full documentation: architecture, the API contract, how the Grad-CAM display
  works, error handling, and known limitations.
- **`AGENT_BRIEF.md`** — paste this into ChatGPT, Claude, Copilot or Cursor to get an AI assistant
  fully briefed on the project so it can help you set it up or extend it.
- **`IMPLEMENTATION_PLAN.md`** — why the app is built the way it is: the design decisions, the
  build order, what was verified, and the known limitations.
