📱 BLE Smart Attendance & Classroom Management App

Kotlin • Bluetooth Low Energy (BLE) • Supabase • Android

🔍 Overview

This project is a BLE-powered smart attendance & classroom management application designed to automate student attendance and improve classroom workflows. The app detects nearby student devices using Bluetooth Low Energy and securely records presence without manual roll calls.

Along with attendance, the app also supports:

Weekly class scheduling

Team/group-based organization

Assignment workflows

Separate dashboards for teachers and students

This is my first full mobile project — built to learn real-world Android development, BLE communication, backend integration, and app system design. The system is mostly functional and demonstrates practical implementation of BLE attendance at scale.

✨ Key Features
✔ BLE Proximity Attendance

Automatically detects nearby student devices and marks attendance

✔ Secure Authentication & Cloud Sync

Powered by Supabase (Auth + Database)

✔ Offline-First Support

Local storage using Room / SharedPreferences

👨‍🏫 Teacher Dashboard

Start attendance session

View present / absent students

Manage classes & reports

👨‍🎓 Student Dashboard

Join classes

View attendance history

Check weekly schedule

📆 Extra Features

Weekly Class Scheduler

Team / Group Management

Assignment Module (Create + Submission Tracking)

🛠 Tech Stack

Language: Kotlin (Android Native)

Backend: Supabase (Auth + DB)

Local Storage: Room / SharedPreferences

Connectivity: Bluetooth Low Energy (BLE APIs)

Architecture: MVVM (if not used, remove this line)

⚙️ How It Works

1️⃣ Teacher starts attendance session
2️⃣ Student devices broadcast unique BLE identity
3️⃣ Teacher app scans nearby BLE devices
4️⃣ Valid students are verified & marked present
5️⃣ Data saved locally → synced to cloud when online

⚠️ Known Limitations

Like real BLE systems, this project has practical constraints:

Older Android devices may fail BLE scanning

Some manufacturers handle BLE differently → inconsistent behavior

Many simultaneous device scans may fail due to Bluetooth hardware limits

Face Recognition attendance planned but not added yet

Performance & stability still being improved

🚧 Project Status

🟢 Core attendance + dashboards working
🟡 Optimization & scalability ongoing

🔜 Planned Enhancements

Face recognition + BLE hybrid attendance

Better handling for multiple simultaneous connections

Improved UI / UX

Push notifications

🎯 What I Learned

Practical BLE implementation & challenges

Android development in Kotlin

Cloud backend integration with Supabase

Offline-first app design

Designing teacher & student workflows

🤝 Acknowledgment

Built independently as part of my learning journey, with research references and occasional AI assistance for debugging and architectural help.

📝 Disclaimer

This is a learning & experimental project. Behavior may vary across devices.
