# Flutter Zipline Plugin Integration Guide

This comprehensive guide walks you through integrating the Flutter Zipline Plugin into your Flutter application, from initial setup to production deployment.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Installation](#installation)
3. [Creating Zipline Modules](#creating-zipline-modules)
4. [Manifest Creation](#manifest-creation)
5. [Basic Integration](#basic-integration)
6. [Advanced Patterns](#advanced-patterns)
7. [Testing Strategies](#testing-strategies)
8. [Performance Optimization](#performance-optimization)
9. [Security Best Practices](#security-best-practices)
10. [Deployment](#deployment)
11. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Required Tools

- **Flutter SDK**: 3.0.0 or higher
- **Dart SDK**: 3.0.0 or higher (included with Flutter)
- **Android Studio** or **VS Code** with Flutter extensions
- **Android SDK**: API level 21 or higher
- **Zipline Compiler**: For creating .zipline modules

### Knowledge Requirements

- Basic understanding of Flutter and Dart
- Familiarity with asynchronous programming (async/await)
- Basic understanding of JSON
- (Optional) JavaScript/TypeScript for writing Zipline modules

---

## Installation

### 1. Add Dependency

Add the plugin to your `pubspec.yaml`:

```yaml
dependencies:
  flutter:
    sdk: flutter
  flutter_zipline_plugin: ^1.0.0
```

Run:
```bash
flutter pub get
```

### 2. Configure Android

Ensure your `android/app/build.gradle` has the minimum SDK version:

```gradle
android {
    defaultConfig {
        applicationId "com.example.myapp"
        minSdkVersion 21  // Required for Zipline
        targetSdkVersion 34
        // ...
    }
}
```

### 3. Add Internet Permission

Add to `android/app/src/main/AndroidManifest.xml`:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.INTERNET" />
    <!-- ... -->
</manifest>
```

### 4. Verify Installation

Create a simple test:

```dart
import 'package:flutter_zipline_plugin/flutter_zipline_plugin.dart';

void main() {
  final plugin = ZiplinePlugin();
  print('Plugin initialized: ${plugin != null}');
}
```

---

## Creating Zipline Modules

### Setting Up Zipline Development

1. **Install Node.js** (for Zipline compiler)
2. **Install Zipline CLI**:
   ```bash
   npm install -g @cashapp/zipline
   ```

### Writing a Simple Module

Create `hello.ts`:

```typescript
// hello.ts - A simple Zipline module

export function initialize() {
  console.log('Hello module initialized');
}

export function greet(name: string): string {
  return `Hello, ${name}!`;
}

export function add(a: number, b: number): number {
  return a + b;
}
```

### Compiling to Zipline

```bash
zipline compile hello.ts -o hello.zipline
```

This creates `hello.zipline` bytecode file.

### Computing SHA256 Hash

```bash
# On Linux/Mac
sha256sum hello.zipline

# On Windows (PowerShell)
Get-FileHash hello.zipline -Algorithm SHA256
```

Save this hash for the manifest.

### Module with Dependencies

Create `utils.ts`:

```typescript
// utils.ts - Utility module
export function formatDate(timestamp: number): string {
  return new Date(timestamp).toISOString();
}
```

Create `main.ts` that depends on `utils.ts`:

```typescript
// main.ts - Main module
import { formatDate } from './utils';

export function initialize() {
  console.log('Main module initialized');
}

export function getCurrentTime(): string {
  return formatDate(Date.now());
}
```

Compile both:
```bash
zipline compile utils.ts -o utils.zipline
zipline compile main.ts -o main.zipline
```

---

## Manifest Creation

### Basic Manifest Structure

Create `manifest.zipline.json`:

```json
{
  "version": "1.0.0",
  "mainModuleId": "hello",
  "mainFunction": "initialize",
  "modules": {
    "hello": {
      "url": "https://example.com/modules/hello.zipline",
      "sha256": "abc123...",
      "dependsOnIds": []
    }
  }
}
```

### Manifest with Dependencies

```json
{
  "version": "1.0.0",
  "mainModuleId": "main",
  "mainFunction": "initialize",
  "modules": {
    "utils": {
      "url": "https://example.com/modules/utils.zipline",
      "sha256": "def456...",
      "dependsOnIds": []
    },
    "main": {
      "url": "https://example.com/modules/main.zipline",
      "sha256": "ghi789...",
      "dependsOnIds": ["utils"]
    }
  }
}
```

### Manifest Validation

Validate your manifest:

```dart
import 'dart:convert';
import 'dart:io';

void validateManifest(String manifestPath) {
  final file = File(manifestPath);
  final json = jsonDecode(file.readAsStringSync());
  
  // Check required fields
  assert(json['version'] != null, 'Missing version');
  assert(json['mainModuleId'] != null, 'Missing mainModuleId');
  assert(json['mainFunction'] != null, 'Missing mainFunction');
  assert(json['modules'] != null, 'Missing modules');
  
  // Check each module
  final modules = json['modules'] as Map<String, dynamic>;
  for (final entry in modules.entries) {
    final module = entry.value as Map<String, dynamic>;
    assert(module['url'] != null, 'Missing url for ${entry.key}');
    assert(module['sha256'] != null, 'Missing sha256 for ${entry.key}');
    assert(module['dependsOnIds'] != null, 'Missing dependsOnIds for ${entry.key}');
    
    // Validate SHA256 format (64 hex characters)
    final sha256 = module['sha256'] as String;
    assert(RegExp(r'^[a-f0-9]{64}$').hasMatch(sha256),
           'Invalid SHA256 for ${entry.key}');
  }
  
  print('Manifest is valid!');
}
```

### Hosting Manifests and Modules

**Option 1: Static Web Server**
```bash
# Upload to your web server
scp manifest.zipline.json user@server:/var/www/extensions/
scp *.zipline user@server:/var/www/extensions/modules/
```

**Option 2: CDN (Recommended)**
- Upload to AWS S3, Google Cloud Storage, or Azure Blob Storage
- Enable HTTPS
- Set appropriate CORS headers

**Option 3: GitHub Releases**
- Create a GitHub release
- Attach manifest and module files
- Use raw.githubusercontent.com URLs

---

## Basic Integration

### Step 1: Create Extension Manager

```dart
// lib/services/extension_manager.dart
import 'package:flutter_zipline_plugin/flutter_zipline_plugin.dart';

class ExtensionManager {
  final ZiplinePlugin _plugin = ZiplinePlugin();
  final Map<String, String> _extensions = {}; // name -> extensionId
  
  /// Stream of lifecycle events
  Stream<ZiplineEvent> get events => _plugin.events;
  
  /// Load an extension by name
  Future<void> loadExtension(String name, String manifestUrl) async {
    if (_extensions.containsKey(name)) {
      throw Exception('Extension $name already loaded');
    }
    
    final extensionId = await _plugin.loadExtension(manifestUrl);
    _extensions[name] = extensionId;
  }
  
  /// Call a function in a loaded extension
  Future<dynamic> callFunction(
    String extensionName,
    String functionName,
    Map<String, dynamic> arguments,
  ) async {
    final extensionId = _extensions[extensionName];
    if (extensionId == null) {
      throw Exception('Extension $extensionName not loaded');
    }
    
    return await _plugin.callFunction(extensionId, functionName, arguments);
  }
  
  /// Unload an extension by name
  Future<void> unloadExtension(String name) async {
    final extensionId = _extensions.remove(name);
    if (extensionId != null) {
      await _plugin.unloadExtension(extensionId);
    }
  }
  
  /// Unload all extensions
  Future<void> unloadAll() async {
    for (final extensionId in _extensions.values) {
      await _plugin.unloadExtension(extensionId);
    }
    _extensions.clear();
  }
  
  /// Check if an extension is loaded
  bool isLoaded(String name) => _extensions.containsKey(name);
  
  /// Get all loaded extension names
  List<String> get loadedExtensions => _extensions.keys.toList();
}
```

### Step 2: Integrate into App

```dart
// lib/main.dart
import 'package:flutter/material.dart';
import 'services/extension_manager.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  final _extensionManager = ExtensionManager();
  bool _isLoading = false;
  String _status = 'Not loaded';
  
  @override
  void initState() {
    super.initState();
    _listenToEvents();
  }
  
  void _listenToEvents() {
    _extensionManager.events.listen((event) {
      setState(() {
        _status = '${event.type}: ${event.message ?? event.moduleName ?? ''}';
      });
    });
  }
  
  Future<void> _loadExtension() async {
    setState(() {
      _isLoading = true;
    });
    
    try {
      await _extensionManager.loadExtension(
        'myExtension',
        'https://example.com/manifest.zipline.json',
      );
      
      setState(() {
        _status = 'Extension loaded successfully';
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _status = 'Error: $e';
        _isLoading = false;
      });
    }
  }
  
  Future<void> _callFunction() async {
    try {
      final result = await _extensionManager.callFunction(
        'myExtension',
        'greet',
        {'name': 'Flutter'},
      );
      
      setState(() {
        _status = 'Result: $result';
      });
    } catch (e) {
      setState(() {
        _status = 'Error: $e';
      });
    }
  }
  
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(title: Text('Zipline Plugin Demo')),
        body: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text(_status),
              SizedBox(height: 20),
              ElevatedButton(
                onPressed: _isLoading ? null : _loadExtension,
                child: Text(_isLoading ? 'Loading...' : 'Load Extension'),
              ),
              SizedBox(height: 10),
              ElevatedButton(
                onPressed: _extensionManager.isLoaded('myExtension')
                    ? _callFunction
                    : null,
                child: Text('Call Function'),
              ),
            ],
          ),
        ),
      ),
    );
  }
  
  @override
  void dispose() {
    _extensionManager.unloadAll();
    super.dispose();
  }
}
```

---

## Advanced Patterns

### Pattern 1: Extension Registry

```dart
// lib/services/extension_registry.dart
class ExtensionConfig {
  final String name;
  final String manifestUrl;
  final String description;
  final bool autoLoad;
  
  ExtensionConfig({
    required this.name,
    required this.manifestUrl,
    required this.description,
    this.autoLoad = false,
  });
}

class ExtensionRegistry {
  final ExtensionManager _manager;
  final List<ExtensionConfig> _configs = [];
  
  ExtensionRegistry(this._manager);
  
  void register(ExtensionConfig config) {
    _configs.add(config);
  }
  
  Future<void> loadAll({bool autoLoadOnly = false}) async {
    for (final config in _configs) {
      if (!autoLoadOnly || config.autoLoad) {
        try {
          await _manager.loadExtension(config.name, config.manifestUrl);
        } catch (e) {
          print('Failed to load ${config.name}: $e');
        }
      }
    }
  }
  
  List<ExtensionConfig> get availableExtensions => _configs;
}

// Usage
final registry = ExtensionRegistry(extensionManager);

registry.register(ExtensionConfig(
  name: 'youtube',
  manifestUrl: 'https://example.com/youtube/manifest.zipline.json',
  description: 'YouTube search extension',
  autoLoad: true,
));

registry.register(ExtensionConfig(
  name: 'spotify',
  manifestUrl: 'https://example.com/spotify/manifest.zipline.json',
  description: 'Spotify search extension',
  autoLoad: false,
));

await registry.loadAll(autoLoadOnly: true);
```

### Pattern 2: Retry Logic

```dart
// lib/utils/retry_helper.dart
class RetryHelper {
  static Future<T> retry<T>(
    Future<T> Function() operation, {
    int maxAttempts = 3,
    Duration delay = const Duration(seconds: 2),
    bool Function(Exception)? shouldRetry,
  }) async {
    var attempt = 0;
    
    while (true) {
      try {
        return await operation();
      } on Exception catch (e) {
        attempt++;
        
        if (attempt >= maxAttempts) {
          rethrow;
        }
        
        if (shouldRetry != null && !shouldRetry(e)) {
          rethrow;
        }
        
        await Future.delayed(delay * attempt);
      }
    }
  }
}

// Usage
final extensionId = await RetryHelper.retry(
  () => plugin.loadExtension(manifestUrl),
  maxAttempts: 3,
  shouldRetry: (e) {
    // Retry on network errors, but not on verification errors
    return e is PlatformException &&
           (e.code == 'ManifestDownloadException' ||
            e.code == 'ModuleDownloadException');
  },
);
```

### Pattern 3: Progress Tracking

```dart
// lib/models/loading_progress.dart
class LoadingProgress {
  final int totalSteps;
  final int completedSteps;
  final String currentStep;
  final double progress;
  
  LoadingProgress({
    required this.totalSteps,
    required this.completedSteps,
    required this.currentStep,
  }) : progress = completedSteps / totalSteps;
  
  bool get isComplete => completedSteps >= totalSteps;
}

class ProgressTracker {
  final _controller = StreamController<LoadingProgress>.broadcast();
  int _totalSteps = 0;
  int _completedSteps = 0;
  
  Stream<LoadingProgress> get progress => _controller.stream;
  
  void startTracking(ZiplinePlugin plugin, int moduleCount) {
    _totalSteps = 2 + (moduleCount * 2); // manifest + modules * 2
    _completedSteps = 0;
    
    plugin.events.listen((event) {
      switch (event.type) {
        case 'manifest_parsed':
          _completedSteps++;
          _emit('Manifest parsed');
          break;
        case 'module_downloaded':
          _completedSteps++;
          _emit('Downloaded ${event.moduleName}');
          break;
        case 'module_loaded':
          _completedSteps++;
          _emit('Loaded ${event.moduleName}');
          break;
        case 'extension_ready':
          _completedSteps = _totalSteps;
          _emit('Extension ready');
          break;
      }
    });
  }
  
  void _emit(String step) {
    _controller.add(LoadingProgress(
      totalSteps: _totalSteps,
      completedSteps: _completedSteps,
      currentStep: step,
    ));
  }
  
  void dispose() {
    _controller.close();
  }
}

// Usage in UI
StreamBuilder<LoadingProgress>(
  stream: progressTracker.progress,
  builder: (context, snapshot) {
    if (!snapshot.hasData) {
      return CircularProgressIndicator();
    }
    
    final progress = snapshot.data!;
    return Column(
      children: [
        LinearProgressIndicator(value: progress.progress),
        Text('${(progress.progress * 100).toInt()}%'),
        Text(progress.currentStep),
      ],
    );
  },
)
```

### Pattern 4: Function Call Wrapper

```dart
// lib/services/extension_wrapper.dart
class ExtensionWrapper {
  final ExtensionManager _manager;
  final String _extensionName;
  
  ExtensionWrapper(this._manager, this._extensionName);
  
  Future<T> call<T>(
    String functionName,
    Map<String, dynamic> arguments,
  ) async {
    try {
      final result = await _manager.callFunction(
        _extensionName,
        functionName,
        arguments,
      );
      return result as T;
    } on PlatformException catch (e) {
      throw ExtensionCallException(
        extensionName: _extensionName,
        functionName: functionName,
        originalError: e,
      );
    }
  }
}

class ExtensionCallException implements Exception {
  final String extensionName;
  final String functionName;
  final PlatformException originalError;
  
  ExtensionCallException({
    required this.extensionName,
    required this.functionName,
    required this.originalError,
  });
  
  @override
  String toString() {
    return 'ExtensionCallException: Failed to call $functionName in $extensionName: ${originalError.message}';
  }
}

// Usage
final youtube = ExtensionWrapper(manager, 'youtube');
final results = await youtube.call<List>('search', {'query': 'flutter'});
```

---

## Testing Strategies

### Unit Testing Extensions

```dart
// test/extension_manager_test.dart
import 'package:flutter_test/flutter_test.dart';
import 'package:mockito/mockito.dart';

class MockZiplinePlugin extends Mock implements ZiplinePlugin {}

void main() {
  group('ExtensionManager', () {
    late ExtensionManager manager;
    late MockZiplinePlugin mockPlugin;
    
    setUp(() {
      mockPlugin = MockZiplinePlugin();
      manager = ExtensionManager(plugin: mockPlugin);
    });
    
    test('loadExtension stores extension ID', () async {
      when(mockPlugin.loadExtension(any))
          .thenAnswer((_) async => 'test-id-123');
      
      await manager.loadExtension('test', 'https://example.com/manifest.json');
      
      expect(manager.isLoaded('test'), true);
      verify(mockPlugin.loadExtension('https://example.com/manifest.json')).called(1);
    });
    
    test('callFunction throws if extension not loaded', () async {
      expect(
        () => manager.callFunction('test', 'func', {}),
        throwsException,
      );
    });
  });
}
```

### Integration Testing

```dart
// integration_test/extension_integration_test.dart
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();
  
  group('Extension Integration Tests', () {
    testWidgets('Load and call extension', (tester) async {
      final plugin = ZiplinePlugin();
      
      // Load extension
      final extensionId = await plugin.loadExtension(
        'https://example.com/test-manifest.zipline.json',
      );
      
      expect(extensionId, isNotEmpty);
      
      // Call function
      final result = await plugin.callFunction(
        extensionId,
        'greet',
        {'name': 'Test'},
      );
      
      expect(result, equals('Hello, Test!'));
      
      // Unload
      await plugin.unloadExtension(extensionId);
    });
  });
}
```

---

## Performance Optimization

### 1. Preload Extensions

```dart
class AppInitializer {
  static Future<void> initialize() async {
    final manager = ExtensionManager();
    
    // Preload critical extensions during splash screen
    await Future.wait([
      manager.loadExtension('youtube', youtubeManifestUrl),
      manager.loadExtension('spotify', spotifyManifestUrl),
    ]);
  }
}

// In main.dart
void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await AppInitializer.initialize();
  runApp(MyApp());
}
```

### 2. Lazy Loading

```dart
class LazyExtensionLoader {
  final ExtensionManager _manager;
  final Map<String, Future<void>> _loadingFutures = {};
  
  LazyExtensionLoader(this._manager);
  
  Future<void> ensureLoaded(String name, String manifestUrl) async {
    if (_manager.isLoaded(name)) {
      return;
    }
    
    // Avoid duplicate loads
    if (_loadingFutures.containsKey(name)) {
      return _loadingFutures[name]!;
    }
    
    final future = _manager.loadExtension(name, manifestUrl);
    _loadingFutures[name] = future;
    
    try {
      await future;
    } finally {
      _loadingFutures.remove(name);
    }
  }
}
```

### 3. Cache Warming

```dart
class CacheWarmer {
  static Future<void> warmCache(List<String> manifestUrls) async {
    final plugin = ZiplinePlugin();
    
    for (final url in manifestUrls) {
      try {
        // Load and immediately unload to populate cache
        final id = await plugin.loadExtension(url);
        await plugin.unloadExtension(id);
      } catch (e) {
        print('Failed to warm cache for $url: $e');
      }
    }
  }
}
```

---

## Security Best Practices

### 1. Validate Manifest URLs

```dart
class ManifestValidator {
  static bool isValidUrl(String url) {
    final uri = Uri.tryParse(url);
    if (uri == null) return false;
    
    // Only allow HTTPS
    if (uri.scheme != 'https') return false;
    
    // Whitelist trusted domains
    final trustedDomains = [
      'example.com',
      'cdn.example.com',
      'extensions.example.com',
    ];
    
    return trustedDomains.any((domain) => uri.host == domain);
  }
}

// Usage
if (!ManifestValidator.isValidUrl(manifestUrl)) {
  throw Exception('Untrusted manifest URL');
}
```

### 2. Handle Verification Failures

```dart
Future<void> loadExtensionSafely(String manifestUrl) async {
  try {
    await plugin.loadExtension(manifestUrl);
  } on PlatformException catch (e) {
    if (e.code == 'ModuleVerificationException') {
      // Security issue - log and alert
      await SecurityLogger.logVerificationFailure(manifestUrl, e.message);
      await showSecurityAlert();
      rethrow;
    }
    // Handle other errors...
  }
}
```

### 3. Implement Rate Limiting

```dart
class RateLimiter {
  final Map<String, DateTime> _lastCalls = {};
  final Duration _minInterval;
  
  RateLimiter({Duration? minInterval})
      : _minInterval = minInterval ?? Duration(seconds: 1);
  
  Future<T> throttle<T>(String key, Future<T> Function() operation) async {
    final lastCall = _lastCalls[key];
    if (lastCall != null) {
      final elapsed = DateTime.now().difference(lastCall);
      if (elapsed < _minInterval) {
        await Future.delayed(_minInterval - elapsed);
      }
    }
    
    _lastCalls[key] = DateTime.now();
    return await operation();
  }
}
```

---

## Deployment

### Production Checklist

- [ ] All manifest URLs use HTTPS
- [ ] SHA256 hashes are verified and correct
- [ ] Manifests are hosted on reliable CDN
- [ ] Error handling is comprehensive
- [ ] Logging is configured for production
- [ ] Performance monitoring is in place
- [ ] Security audit completed
- [ ] Integration tests pass
- [ ] Load testing completed

### Monitoring

```dart
class ExtensionMonitor {
  static void trackLoadTime(String extensionName, Duration duration) {
    // Send to analytics
    Analytics.logEvent('extension_load', {
      'name': extensionName,
      'duration_ms': duration.inMilliseconds,
    });
  }
  
  static void trackError(String extensionName, Exception error) {
    // Send to error tracking
    ErrorTracker.logError(error, {
      'extension': extensionName,
      'type': 'extension_error',
    });
  }
}
```

---

## Troubleshooting

### Common Issues

**Issue**: Extension fails to load with network error

**Solution**:
- Check internet connectivity
- Verify manifest URL is accessible
- Check for firewall/proxy issues
- Implement retry logic

**Issue**: SHA256 verification fails

**Solution**:
- Recompute SHA256 hash of module file
- Update manifest with correct hash
- Clear app cache and retry
- Verify module file wasn't corrupted

**Issue**: Function not found

**Solution**:
- Verify function is exported in module
- Check function name spelling
- Ensure all dependencies are loaded
- Review module compilation output

**Issue**: Out of memory

**Solution**:
- Unload unused extensions
- Reduce number of concurrent extensions
- Profile memory usage
- Optimize module size
