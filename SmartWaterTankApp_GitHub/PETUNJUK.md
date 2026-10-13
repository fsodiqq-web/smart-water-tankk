# Smart Water Tank Android App

Aplikasi Android native untuk membaca data dari Arduino Uno melalui Bluetooth serial HC-05.

## Fungsi
- Memilih perangkat Bluetooth yang sudah dipasangkan
- Menampilkan persentase isi tangki
- Menampilkan volume air (liter), jarak sensor, dan tinggi air
- Status HAMPIR HABIS / SEDANG / PENUH

## Penting
Aplikasi ini membaca format CSV yang dikirim sketch Arduino:
`jarak_cm,tinggi_air_cm,persen,liter,status`
Contoh: `35.2,64.8,68.4,342.0,SEDANG`

Pastikan baud rate HC-05 pada sketch Arduino 9600 dan pasangkan HC-05 di pengaturan Bluetooth Android sebelum membuka aplikasi. PIN umum 1234 atau 0000.

## Membuat APK
Folder ini adalah proyek sumber Android Studio. Untuk menghasilkan APK, buka folder proyek di Android Studio, tunggu Gradle sync, lalu pilih **Build > Build APK(s)**. APK biasanya berada di `app/build/outputs/apk/debug/app-debug.apk`.

Proyek ini disediakan sebagai source karena lingkungan pembuatan saat ini tidak memiliki Android SDK/Gradle yang diperlukan untuk mengompilasi APK langsung.
