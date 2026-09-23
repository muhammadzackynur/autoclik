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

class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> with WidgetsBindingObserver {
  static const platform = MethodChannel('com.example.auto/accessibility');

  bool _accessibilityEnabled = false;
  bool _overlayShown = false;
  bool _aggressiveMode = false;
  bool _checking = true;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _refreshStatus();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    // Setiap kali user balik ke app (misal habis dari Settings), cek ulang statusnya
    if (state == AppLifecycleState.resumed) {
      _refreshStatus();
    }
  }

  Future<void> _refreshStatus() async {
    setState(() => _checking = true);
    bool enabled = false;
    try {
      enabled = await platform.invokeMethod('isAccessibilityServiceEnabled');
    } on PlatformException catch (e) {
      debugPrint("Gagal cek status accessibility: '${e.message}'.");
    }
    bool aggressive = false;
    try {
      aggressive = await platform.invokeMethod('getAggressiveMode');
    } on PlatformException catch (e) {
      debugPrint("Gagal ambil status mode agresif: '${e.message}'.");
    }

    setState(() {
      _accessibilityEnabled = enabled;
      _aggressiveMode = aggressive;
      _checking = false;
    });
  }

  Future<void> _setAggressiveMode(bool value) async {
    try {
      await platform.invokeMethod('setAggressiveMode', {'enabled': value});
      setState(() => _aggressiveMode = value);
    } on PlatformException catch (e) {
      debugPrint("Gagal ubah mode agresif: '${e.message}'.");
    }
  }

  Future<void> _openAccessibilitySettings() async {
    try {
      await platform.invokeMethod('openAccessibilitySettings');
    } on PlatformException catch (e) {
      debugPrint("Gagal membuka pengaturan aksesibilitas: '${e.message}'.");
    }
  }

  Future<void> _toggleOverlay() async {
    if (!_overlayShown) {
      try {
        final hasPermission =
            await platform.invokeMethod('checkOverlayPermission') as bool;
        if (!hasPermission) {
          await platform.invokeMethod('requestOverlayPermission');
          if (!mounted) return;
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(
              content: Text(
                'Izinkan "Tampil di atas aplikasi lain" lalu kembali ke sini.',
              ),
            ),
          );
          return;
        }
        await platform.invokeMethod('startOverlayService');
        setState(() => _overlayShown = true);
      } on PlatformException catch (e) {
        debugPrint("Gagal menampilkan bubble: '${e.message}'.");
      }
    } else {
      try {
        await platform.invokeMethod('stopOverlayService');
        setState(() => _overlayShown = false);
      } on PlatformException catch (e) {
        debugPrint("Gagal menutup bubble: '${e.message}'.");
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Auto Skip Iklan Game'),
        centerTitle: true,
      ),
      body: RefreshIndicator(
        onRefresh: _refreshStatus,
        child: ListView(
          padding: const EdgeInsets.all(24.0),
          children: [
            const SizedBox(height: 12),
            const Icon(Icons.touch_app, size: 80, color: Colors.blue),
            const SizedBox(height: 24),
            const Text(
              'Layanan Otomatis Skip Iklan',
              textAlign: TextAlign.center,
              style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 20),

            // Indikator status
            Container(
              padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 16),
              decoration: BoxDecoration(
                color: (_accessibilityEnabled ? Colors.green : Colors.red)
                    .withOpacity(0.1),
                borderRadius: BorderRadius.circular(12),
                border: Border.all(
                  color: _accessibilityEnabled ? Colors.green : Colors.red,
                ),
              ),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  if (_checking)
                    const SizedBox(
                      width: 14,
                      height: 14,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  else
                    Container(
                      width: 14,
                      height: 14,
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        color: _accessibilityEnabled
                            ? Colors.green
                            : Colors.red,
                      ),
                    ),
                  const SizedBox(width: 10),
                  Text(
                    _checking
                        ? 'Mengecek status...'
                        : (_accessibilityEnabled
                              ? 'Layanan Aktif'
                              : 'Layanan Nonaktif'),
                    style: const TextStyle(
                      fontSize: 15,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 12),

            const Text(
              'Aktifkan Layanan Aksesibilitas di pengaturan sistem ponsel Anda agar aplikasi dapat mendeteksi tombol skip atau ikon silang secara otomatis.',
              textAlign: TextAlign.center,
              style: TextStyle(fontSize: 14, color: Colors.grey),
            ),
            const SizedBox(height: 24),

            ElevatedButton.icon(
              style: ElevatedButton.styleFrom(
                padding: const EdgeInsets.symmetric(vertical: 16),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
              ),
              onPressed: _openAccessibilitySettings,
              icon: const Icon(Icons.settings_accessibility),
              label: const Text(
                'Buka Pengaturan Aksesibilitas',
                style: TextStyle(fontSize: 16),
              ),
            ),
            const SizedBox(height: 12),

            OutlinedButton.icon(
              style: OutlinedButton.styleFrom(
                padding: const EdgeInsets.symmetric(vertical: 16),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
              ),
              onPressed: _toggleOverlay,
              icon: Icon(
                _overlayShown ? Icons.visibility_off : Icons.visibility,
              ),
              label: Text(
                _overlayShown
                    ? 'Sembunyikan Bubble ON/OFF'
                    : 'Tampilkan Bubble ON/OFF di Layar',
                style: const TextStyle(fontSize: 16),
              ),
            ),
            const SizedBox(height: 8),
            const Text(
              'Bubble kecil akan muncul melayang di layar. Tap untuk ON/OFF, geser untuk pindah posisi.',
              textAlign: TextAlign.center,
              style: TextStyle(fontSize: 12, color: Colors.grey),
            ),

            const SizedBox(height: 28),
            const Divider(),
            const SizedBox(height: 12),

            SwitchListTile(
              contentPadding: EdgeInsets.zero,
              title: const Text(
                'Mode Agresif (tap pojok layar)',
                style: TextStyle(fontWeight: FontWeight.w600),
              ),
              subtitle: const Text(
                'Kalau tombol X/skip iklan berupa gambar di dalam WebView '
                'dan tidak kedeteksi otomatis, aplikasi akan coba tap buta '
                'di pojok kanan-atas & kiri-atas layar. Berisiko salah tap '
                'di layar lain, aktifkan dengan hati-hati.',
                style: TextStyle(fontSize: 12, color: Colors.grey),
              ),
              value: _aggressiveMode,
              onChanged: _setAggressiveMode,
            ),
          ],
        ),
      ),
    );
  }
}
