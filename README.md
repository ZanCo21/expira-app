# Expira

Aplikasi Android **offline** yang membaca tanggal kedaluwarsa dari foto kemasan makanan. Foto dari
kamera atau galeri diperbaiki dengan *preprocessing* citra (OpenCV), dibaca dengan OCR on-device
(Google ML Kit), lalu tanggalnya diekstrak, divalidasi, dan diberi skor keyakinan. Pengguna dapat
menerima, mengonfirmasi, atau mengoreksi hasil secara manual, menyimpannya ke riwayat, dan memasang
pengingat sebelum produk kedaluwarsa.

Proyek ini adalah implementasi skripsi *"Implementasi OCR dan Preprocessing Citra untuk Pembacaan
Tanggal Kedaluwarsa Kemasan Makanan Berbasis Android"*. Kontribusi penelitiannya ada pada pipeline
preprocessing, ekstraksi pola tanggal, dan validasi hasil:

```
foto -> preprocessing -> OCR -> ekstraksi tanggal -> validasi -> hasil / koreksi manual
```

## Fitur

| ID | Fitur | Prioritas |
|---|---|---|
| F-01, F-02 | Ambil foto (dipotong sesuai bingkai di layar) atau pilih dari galeri | Must |
| F-03 | Preprocessing P0-P3 (grayscale, CLAHE, median, adaptive threshold, deskew/sharpen/closing kondisional) | Must |
| F-04 | OCR on-device ML Kit Text Recognition v2 (model *bundled*, tanpa unduhan) | Must |
| F-05 | Ekstraksi tanggal: 8 format, bulan Indonesia/Inggris, kata kunci EXP vs MFD | Must |
| F-06 | Validasi kalender + rentang tahun, skor keyakinan 4 komponen | Must |
| F-07, F-08 | Layar Hasil / Konfirmasi / Gagal, koreksi manual, foto ulang, status Aman/Mendekati/Kedaluwarsa | Must |
| F-09, F-12 | Mode Evaluasi P0-P3 dengan ground truth, log waktu, ekspor CSV | Should / Could |
| F-10 | Riwayat lokal dengan filter status, hapus entri (tekan lama) | Should |
| F-11 | Bingkai panduan foto dan peringatan blur langsung di kamera | Should |
| F-13, F-14 | Pengingat H-7/H-3/H-1 (WorkManager) dan nama produk opsional | Tambahan |

Semua data (foto, riwayat, log evaluasi) tersimpan di ponsel. Aplikasi tidak memakai server, akun,
maupun koneksi internet; izin `INTERNET` yang dibawa telemetri ML Kit sengaja dihapus dari manifest.

## Teknologi

| Komponen | Teknologi |
|---|---|
| Bahasa / UI | Kotlin 2.2, Jetpack Compose (Material 3), font Inter |
| Arsitektur | MVVM, Coroutines, StateFlow, DI manual (`AppContainer`) |
| Kamera | CameraX 1.4 (`LifecycleCameraController`, `ImageAnalysis` untuk deteksi blur) |
| Preprocessing | OpenCV for Android 4.12 (AAR resmi Maven Central) |
| OCR | Google ML Kit Text Recognition v2 16.0.1 (bundled) - satu-satunya mesin OCR |
| Penyimpanan | Room 2.7 (skema diekspor di `app/schemas/`) |
| Pengingat | WorkManager 2.10 + NotificationManager |
| Splash | `androidx.core:core-splashscreen` |
| Min / target SDK | 24 (Android 7.0) / 36 |

## Menjalankan proyek

Prasyarat: JDK 17, Android SDK (platform 36), emulator atau ponsel Android 7.0+.
Buat `local.properties` berisi `sdk.dir=...` bila Android Studio belum membuatnya.

```bash
./gradlew assembleDebug            # build APK debug -> app/build/outputs/apk/debug/
./gradlew installDebug             # pasang ke emulator/ponsel yang terhubung
./gradlew testDebugUnitTest        # unit test JVM (logika murni)
./gradlew connectedDebugAndroidTest # instrumented test (perlu emulator/ponsel)
./gradlew lintDebug                # pemeriksaan lint
```

> `connectedDebugAndroidTest` menghapus instalasi aplikasi setelah selesai; jalankan
> `./gradlew installDebug` lagi untuk mencoba aplikasinya.

## Pengujian

| Jenis | Jumlah | Cakupan |
|---|---|---|
| Unit test JVM | 102 | Format tanggal, koreksi O/0 dan I/l/S/B, tanggal tidak valid & kabisat, MFD vs EXP, validator, skor keyakinan, status Aman/Mendekati, pemilihan varian, metrik evaluasi (CER, exact-match), format CSV, perencanaan pengingat, aritmetika tanggal |
| Instrumented | 24 | OpenCV P0-P3 (deskew, closing, sharpen, blur, citra hitam), pipeline ML Kit end-to-end, Room & migrasi skema v1->v3, penjadwalan/pembatalan WorkManager, isi notifikasi |

Diuji di emulator **Android 7.0 (API 24)** dan **API 37**: kamera, galeri, alur Hasil/Konfirmasi/Gagal,
koreksi, riwayat, pengingat + notifikasi, panduan baterai, Mode Evaluasi, dan ekspor CSV.

Mencoba notifikasi pengingat tanpa menunggu tanggalnya (job WorkManager dipaksa jalan):

```bash
# Android 14+ (job WorkManager memakai namespace)
adb shell cmd jobscheduler run -f -n androidx.work.systemjobscheduler com.expira.app 0
# Android 7-13: cari ID job SystemJobService milik com.expira.app, lalu
adb shell dumpsys jobscheduler | grep "com.expira.app/androidx.work"
adb shell cmd jobscheduler run -f com.expira.app <id>
```

Skor blur kamera dicatat di build debug untuk kalibrasi: `adb logcat -s BlurAnalyzer`.

## Struktur modul

```
app/src/main/java/com/expira/app/
  ui/           layar Compose + ViewModel (home, camera, processing, review, correction,
                history, evaluation, reminder) dan komponen bersama
  domain/       ScanDateUseCase, CorrectDateUseCase, SaveScanUseCase, ScheduleReminderUseCase,
                DeleteScanUseCase, status kedaluwarsa, perencanaan pengingat
  imaging/      ImagePreprocessor (OpenCV), BlurDetector, pemuat & pemotong citra
  ocr/          OcrEngine (ML Kit)
  extraction/   DateExtractor, DateValidator, ConfidenceScorer  <- Kotlin murni, tanpa android.*
  data/         Room (riwayat, log evaluasi), WorkManager reminder, notifikasi
  evaluation/   RunEvaluationUseCase, metrik, ekspor CSV
```

Paket `extraction/` tidak mengimpor apa pun dari Android sehingga seluruh aturannya diuji dengan
unit test JVM biasa.

## Pipeline penelitian

### Konfigurasi preprocessing

| Kode | Isi |
|---|---|
| P0 | Tanpa preprocessing (baseline) |
| P1 | Grayscale + CLAHE |
| P2 | P1 + median ringan + adaptive threshold |
| P3 | P2 + sharpen (jika buram) / deskew (jika miring >= 2 derajat) / closing (jika cetakan dot-matrix) |

Semua parameter dan ambang ada di `imaging/PreprocessConfig.kt` (`PreprocessParams`). Pemindaian
biasa menjalankan P0 -> P1 -> P3 (maksimal 3 varian) dan berhenti lebih awal bila sudah ada kata
kunci EXP pada baris yang sama dengan tanggal lengkap. Mode Evaluasi menjalankan P0, P1, P2, dan P3
masing-masing secara terpisah pada citra yang sama.

### Ekstraksi dan validasi

- Kata kunci kedaluwarsa: EXP, EXPIRY, ED, E.D., BB, BBE, BEST BEFORE, USE BY, KEDALUWARSA, KADALUWARSA,
  BAIK DIGUNAKAN SEBELUM. Kata kunci produksi: MFD, PROD, PD, TGL PROD, BATCH, LOT.
- Format: `DD/MM/YYYY`, `DD-MM-YY`, `DD.MM.YYYY`, `DD MMM YYYY`, `DDMMMYY`, `MMM YYYY`, `MM/YYYY`, `YYYY-MM-DD`.
- `MMM YYYY` dan `MM/YYYY` memakai **hari terakhir bulan** (FEB 2028 -> 29-02-2028). Ground truth
  dataset harus mengikuti aturan yang sama.
- Koreksi karakter hanya pada konteks angka: O->0, I/l->1, S->5, B->8.
- Tanggal yang tidak ada di kalender ditolak; tahun harus dalam rentang tahun pemindaian -5 s.d. +10.
- Keluaran seragam `DD-MM-YYYY`.

### Tingkat keyakinan

Skor = rata-rata berbobot sama (25%) dari confidence OCR, kecocokan pola, kata kunci di dekat tanggal,
dan kewajaran tanggal (tanpa confidence OCR, tiga komponen lain dibagi rata). Ambang awal:
**>= 0,80** Hasil, **0,50-0,80** Konfirmasi, **< 0,50** Gagal. Semua konstanta ada di
`extraction/ConfidenceScorer.kt` (`ScoringParams`) dan harus dikalibrasi dengan data uji.

## Mode Evaluasi dan log CSV

Tab **Evaluasi** -> *Jalankan Pengujian* -> pilih foto dari aplikasi Files (nama file asli ikut
tercatat) -> isi ground truth `DD-MM-YYYY`. Sebelum pengukuran pertama, OCR "dipanaskan" sekali agar
waktu muat model tidak membebani P0. *Ekspor Log CSV* menyimpan satu file berisi log evaluasi
(`source=evaluation`) dan riwayat pemindaian (`source=scan`):

| Kolom | Keterangan |
|---|---|
| `run_id`, `image`, `config` | Satu pengujian = 4 baris (P0-P3) dengan `run_id` sama |
| `ground_truth`, `result_date`, `detected_date` | Tanggal benar, hasil akhir, dan usulan pipeline |
| `extraction_success`, `exact_match`, `day_match`, `month_match`, `year_match` | 1/0 per baris |
| `cer` | Character error rate tanggal hasil terhadap ground truth (format DD-MM-YYYY) |
| `confidence`, `confidence_level` | Skor 0-1 dan HIGH/MEDIUM/LOW |
| `was_corrected` | Riwayat: tanggal final berbeda dari usulan pipeline (dasar tingkat koreksi manual) |
| `applied_steps`, `blur_score` | Langkah kondisional P3 yang berjalan, skor variance of Laplacian |
| `preprocess_ms`, `ocr_ms`, `extract_validate_ms`, `total_ms` | Waktu per tahap |
| `detected_line`, `raw_ocr_text` | Baris asal tanggal dan teks OCR mentah |

Rata-rata, median, dan p95 waktu serta akurasi per komponen dihitung dari CSV ini.

## Keputusan perilaku

| Hal | Keputusan |
|---|---|
| Status | Kedaluwarsa jika sisa < 0 hari, Mendekati 0-30 hari, sisanya Aman |
| Foto kamera | Dipotong sesuai bingkai di layar (margin 8%); foto galeri diproses utuh |
| Tanpa kata kunci | Tanggal terbaru dipilih; tanggal berkata kunci produksi tidak pernah dipilih |
| Setelah simpan/koreksi | Kembali ke Beranda |
| Konfirmasi | "Ya, Tanggal Sudah Benar" langsung menyimpan tanggal yang dipilih |
| Pengingat | Sekitar pukul 09.00 waktu lokal; tidak dijadwalkan bila harinya sudah lewat atau izin notifikasi ditolak |
| Hapus entri | Tekan lama baris Riwayat -> konfirmasi; foto dan pengingatnya ikut dihapus |
| Baterai | Lembar Pengingat menampilkan panduan mengecualikan Expira dari optimasi baterai selama belum dikecualikan |

## Batasan

- Hasil berlaku untuk mesin OCR ML Kit dan dataset/perangkat yang diuji; angka waktu di atas berasal
  dari emulator dan harus diukur ulang di ponsel nyata.
- Permukaan sangat melengkung, sangat berkilap, atau cetakan hampir hilang tidak dijamin terbaca;
  dewarping tidak dikerjakan.
- Hanya aksara Latin (Indonesia/Inggris). Aplikasi membaca tanggal, bukan menilai keamanan pangan.
- Pengingat bersifat lokal dan dapat tertunda beberapa jam (atau tertahan) pada ponsel dengan
  penghematan baterai agresif.
- Ikon belum memiliki versi monokrom untuk *themed icons* Android 13+.

## Aset

Ikon aplikasi bersumber dari `app/src/main/ic_launcher-source.svg`; turunannya adalah ikon adaptif
(`res/drawable/ic_launcher_foreground.xml`), PNG mipmap untuk API 24-25, dan
`app/src/main/ic_launcher-playstore.png`. Font Inter berlisensi SIL OFL 1.1 (`third_party/inter/LICENSE.txt`).
