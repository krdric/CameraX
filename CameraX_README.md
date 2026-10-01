# 📷 CameraX

**CameraX** is a student-focused monitoring camera application designed
to help maintain attention while studying.

The app opens the device camera and continuously observes the student's
study posture and visible face/eyes. When the student is normally
sitting with the book in front of them and studying, the app stays in a
normal monitoring state.

If the app detects an unwanted change, such as the student's face being
covered by the book or the student's eyes remaining closed, it can
trigger an alarm to alert the student.

> **Note:** The exact detection accuracy depends on the computer-vision
> models and implementation used inside the Android app.

------------------------------------------------------------------------

## 👨‍💻 Author

**Name:** Ric

**Project:** CameraX\
**Platform:** Android\
**Purpose:** Student Study Monitoring / Attention Assistance

------------------------------------------------------------------------

## 🎯 Main Purpose

CameraX is designed to provide a simple study-monitoring experience.

The basic idea is:

**Open Camera → Start Study Session → Monitor Student → Detect Unusual
Behavior → Trigger Alarm**

It can be useful for students who want an extra reminder to stay
attentive while studying.

------------------------------------------------------------------------

## 🔥 How CameraX Works

When a study session starts:

1.  CameraX opens the phone's camera.
2.  The camera continuously captures frames for analysis.
3.  The application checks the student's face and eyes.
4.  Normal study behavior does not trigger an alarm.
5.  If an unwanted condition is detected for a configured amount of
    time, the alarm can start.
6.  The student can correct their position or stop the study session.

### Example

**Normal condition:**

📖 Student → Book in front → Face visible → Eyes open\
➡️ **No alarm**

**Face covered:**

📖 Book moves over the student's face\
➡️ Face becomes unavailable/obstructed\
➡️ **Alarm can be triggered**

**Eyes closed:**

👀 Student's eyes remain closed for the configured duration\
➡️ Possible distraction/drowsiness condition\
➡️ **Alarm can be triggered**

------------------------------------------------------------------------

## 🚨 Detection Conditions

CameraX can be designed to detect conditions such as:

### 1. Face Covered

If the book or another object covers the student's face and the face can
no longer be detected properly, the application can identify this as an
abnormal study condition.

### 2. Eyes Closed

The application can use eye-state detection to identify when the
student's eyes remain closed beyond a configured threshold.

### 3. Face Missing

If the student's face moves outside the camera frame for a certain
period, the application can treat it as a monitoring interruption.

### 4. Normal Study Position

When the face is visible and the eyes are open, the application
continues normal monitoring without activating the alarm.

------------------------------------------------------------------------

## ⏱️ Why a Delay Is Important

The alarm should not start immediately after one incorrect frame.

For example:

``` text
Eyes closed for 0.2 seconds
        ↓
Ignore

Eyes closed for 2 seconds
        ↓
Check condition

Eyes closed for configured duration
        ↓
Trigger alarm
```

This helps reduce false alarms caused by normal actions such as
blinking, moving the head, or adjusting the book.

------------------------------------------------------------------------

## 📱 How to Use CameraX

### Step 1: Open the App

Launch the **CameraX** application.

### Step 2: Give Camera Permission

Allow the application to access the camera.

### Step 3: Start Study Session

Place the phone so that your face and upper body are visible.

### Step 4: Keep the Book in a Normal Position

Read the book normally while keeping your face visible to the camera.

### Step 5: Study Normally

The app continuously monitors the configured conditions.

### Step 6: Alarm

If an unwanted condition is detected for the configured duration, the
alarm sounds.

### Step 7: Stop Session

When studying is finished, stop the monitoring session and close the
camera.

------------------------------------------------------------------------

## 🧠 Possible Technology Stack

The project can use Android computer-vision technologies such as:

-   **Android Studio**
-   **Kotlin / Java**
-   **CameraX**
-   **Google ML Kit Face Detection**
-   Face landmark / eye-state detection
-   Android Audio / Media APIs for the alarm
-   Android Runtime Camera Permission
-   Background-safe processing where required

------------------------------------------------------------------------

## 🔐 Permissions

CameraX may require:

``` xml
<uses-permission android:name="android.permission.CAMERA" />
```

If the application uses vibration or notifications for alerts,
additional Android permissions may be required depending on the Android
version and implementation.

------------------------------------------------------------------------

## ⚙️ Basic Detection Logic

A simplified version of the application logic can be represented as:

``` text
Start Camera
     ↓
Capture Camera Frame
     ↓
Detect Face
     ↓
Is Face Visible?
 ┌───┴────┐
 No       Yes
 ↓         ↓
Check     Detect Eyes
Face      ↓
Missing   Eyes Open?
Timer     ↓
 ↓      ┌─┴───┐
Timeout No    Yes
 ↓       ↓      ↓
Alarm   Check   Normal
        Timer   Study
          ↓
        Timeout
          ↓
         Alarm
```

------------------------------------------------------------------------

## 🔔 Alarm Behavior

When an abnormal condition is confirmed, CameraX can:

-   Play an alarm sound
-   Vibrate the phone
-   Show an on-screen warning
-   Display the detected condition
-   Allow the student to stop or dismiss the alarm

Example warning:

``` text
⚠️ ATTENTION

Your study position needs attention.

Please keep your face visible
and continue studying.
```

------------------------------------------------------------------------

## 🎓 Use Case

CameraX can be useful for:

-   Students studying alone
-   Self-study sessions
-   Online learning environments
-   Pomodoro-style study sessions
-   Attention and posture reminders
-   Personal study monitoring

The application should be treated as an **attention-assistance tool**,
not as a replacement for a teacher or human supervision.

------------------------------------------------------------------------

## 📂 Suggested Project Structure

``` text
CameraX/
│
├── app/
│   └── src/
│       └── main/
│           ├── java/
│           │   └── .../
│           │       ├── MainActivity
│           │       ├── CameraManager
│           │       ├── FaceDetector
│           │       ├── EyeDetector
│           │       └── AlarmManager
│           │
│           ├── res/
│           │   ├── drawable/
│           │   ├── mipmap/
│           │   ├── layout/
│           │   └── values/
│           │
│           └── AndroidManifest.xml
│
├── README.md
└── build.gradle
```

> The actual file names may differ depending on the project's
> implementation.

------------------------------------------------------------------------

## 🛠️ Future Improvements

Possible future features include:

-   📊 Study-session statistics
-   ⏱️ Custom study timer
-   🔔 Custom alarm sounds
-   👁️ Improved eye-state detection
-   📖 Better book/face obstruction detection
-   📈 Daily study reports
-   🏆 Study streak system
-   🌙 Dark mode
-   🔋 Battery optimization
-   ⚙️ Adjustable detection sensitivity
-   🎯 Custom alarm delay
-   📱 Modern CameraX interface

------------------------------------------------------------------------

## ⚠️ Privacy

CameraX should process camera data responsibly.

Recommended design:

-   Use the camera only after the user starts a session.
-   Clearly show when camera monitoring is active.
-   Avoid storing camera frames unless explicitly required.
-   Process data locally on the device where possible.
-   Stop camera access when the study session ends.
-   Explain camera usage clearly to the user.

------------------------------------------------------------------------

## 📄 Project Summary

**CameraX** is an Android-based student study-monitoring application
that uses the phone camera and computer-vision techniques to observe
basic study conditions.

Its core concept is simple:

> **If the student is studying normally, everything stays normal. If a
> configured distraction or monitoring interruption is detected, CameraX
> alerts the student.**

------------------------------------------------------------------------

## 👨‍💻 Developer

**Ric**

**Project Name:** CameraX\
**Category:** Android / Computer Vision / Student Productivity

------------------------------------------------------------------------

### ⭐ CameraX

**Study. Stay Focused. Keep Going.**
