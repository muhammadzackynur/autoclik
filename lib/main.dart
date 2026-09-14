import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() {
  runApp(const AutoClickerApp());
}

class AutoClickerApp extends StatelessWidget {
  const AutoClickerApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Auto Skip Iklan',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(primarySwatch: Colors.blue, useMaterial3: true),
      home: const HomePage(),
    );
  }
}

class HomePage extends StatelessWidget {
  const HomePage({super.key});

  static const platform = MethodChannel('com.example.auto/accessibility');

  Future<void> _openAccessibilitySettings() async {
    try {
      await platform.invokeMethod('openAccessibilitySettings');
    } on PlatformException catch (e) {
      debugPrint("Gagal membuka pengaturan aksesibilitas: '${e.message}'.");
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Auto Skip Iklan Game'),
        centerTitle: true,
      ),
      body: Padding(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const Icon(Icons.touch_app, size: 80, color: Colors.blue),
            const SizedBox(height: 24),
            const Text(
              'Layanan Otomatis Skip Iklan',
              textAlign: TextAlign.center,
              style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 12),
            const Text(
              'Aktifkan Layanan Aksesibilitas di pengaturan sistem ponsel Anda agar aplikasi dapat mendeteksi tombol skip atau ikon silang secara otomatis.',
              textAlign: TextAlign.center,
              style: TextStyle(fontSize: 14, color: Colors.grey),
            ),
            const SizedBox(height: 32),
            ElevatedButton(
              style: ElevatedButton.styleFrom(
                padding: const EdgeInsets.symmetric(vertical: 16),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
              ),
              onPressed: _openAccessibilitySettings,
              child: const Text(
                'Buka Pengaturan Aksesibilitas',
                style: TextStyle(fontSize: 16),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
