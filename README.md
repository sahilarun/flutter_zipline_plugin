# Flutter Zipline Plugin

A Flutter plugin that enables dynamic code execution by integrating Cash App's Zipline engine into Flutter applications. Load, execute, and manage Zipline bytecode modules at runtime without app updates.

[![pub package](https://img.shields.io/pub/v/flutter_zipline_plugin.svg)](https://pub.dev/packages/flutter_zipline_plugin)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

## Features

✨ **Dynamic Code Loading** - Download and execute Zipline modules at runtime  
🔒 **Cryptographic Verification** - SHA256 hash verification for all modules  
📦 **Smart Caching** - Local caching for offline operation and faster loads  
🔄 **Dependency Resolution** - Automatic topological sorting of module dependencies  
🚀 **Concurrent Extensions** - Load and run multiple extensions simultaneously  
📡 **Event Streaming** - Real-time progress updates during loading  
🛡️ **Sandboxed Execution** - Isolated Zipline engine instances for security  
⚡ **High Performance** - Optimized for fast function calls and minimal overhead

## Use Cases

- **Plugin Systems**: Build extensible apps like Soundbound with downloadable extensions
- **Feature Flags**: Deploy features dynamically without app store updates
- **A/B Testing**: Test different implementations without redeployment
- **Hot Fixes**: Fix bugs in production without waiting for app review
- **Content Delivery**: Deliver dynamic business logic alongside content

## Platform Support

| Platform | Supported | Version |
|----------|-----------|---------|
| Android  | ✅        | API 21+ (Android 5.0+) |
| iOS      | ❌        | Planned for v2.0 |
| Web      | ❌        | Not planned |
| Desktop  | ❌        | Not planned |

## Installation

Add this to your package's `pubspec.yaml` file:

```yaml
dependencies:
  flutter_zipline_plugin: ^1.0.0
```

Then run:

```bash
flutter pub get
```

### Android Configuration

The plugin requires Android API level 21 or higher. Ensure your `android/app/build.gradle` has:

```gradle
android {
    defaultConfig {
        minSdkVersion 21
        // ...
    }
}
```

## Quick Start

### 1. Create a Plugin Instance

```dart
import 'package:flutter_zipline_plugin/flutter_zipline_plugin.dart';

final plugin = ZiplinePlugin();
```

### 2. Listen to Events (Optional)

```dart
plugin.events.listen((event) {
  print('${event.type}: ${event.message ?? event.moduleName ?? ''}');
});
```

### 3. Load an Extension

```dart
try {
  final extensionId = await plugin.loadExtension(
    'https://example.com/manifest.zipline.json'
  );
  print('Extension loaded: $extensionId');
} catch (e) {
  print('Failed to load extension: $e');
}
```

### 4. Call Functions

```dart
try {
  final result = await plugin.callFunction(
    extensionId,
    'search',
    {'query': 'flutter', 'limit': 10},
  );
  print('Results: $result');
} catch (e) {
  print('Function call failed: $e');
}
```

### 5. Unload Extension

```dart
await plugin.unloadExtension(extensionId);
```

## Complete Example

```dart
import 'package:flutter/material.dart';
import 'package:flutter_zipline_plugin/flutter_zipline_plugin.dart';

class ExtensionManager {
  final _plugin = ZiplinePlugin();
  String? _extensionId;
  
  Stream<ZiplineEvent> get events => _plugin.events;
  
  Future<void> loadExtension(String manifestUrl) async {
    _extensionId = await _plugin.loadExtension(manifestUrl);
  }
  
  Future<List<dynamic>> search(String query) async {
    if (_extensionId == null) {
      throw Exception('Extension not loaded');
    }
    
    final result = await _plugin.callFunction(
      _extensionId!,
      'search',
      {'query': query, 'limit': 20},
    );
    
    return result['items'] as List<dynamic>;
  }
  
  Future<void> unload() async {
    if (_extensionId != null) {
      await _plugin.unloadExtension(_extensionId!);
      _extensionId = null;
    }
  }
}

// Usage in a widget
class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  final _manager = ExtensionManager();
  List<dynamic> _results = [];
  
  @override
  void initState() {
    super.initState();
    _loadExtension();
  }
  
  Future<void> _loadExtension() async {
    try {
      await _manager.loadExtension(
        'https://example.com/manifest.zipline.json'
      );
      setState(() {});
    } catch (e) {
      print('Error: $e');
    }
  }
  
  Future<void> _search(String query) async {
    try {
      final results = await _manager.search(query);
      setState(() {
        _results = results;
      });
    } catch (e) {
      print('Search error: $e');
    }
  }
  
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(title: Text('Zipline Plugin Demo')),
        body: ListView.builder(
          itemCount: _results.length,
          itemBuilder: (context, index) {
            final item = _results[index];
            return ListTile(
              title: Text(item['title'] ?? ''),
              subtitle: Text(item['description'] ?? ''),
            );
          },
        ),
      ),
    );
  }
}
```

## Manifest Format

Extensions are defined by a `manifest.zipline.json` file:

```json
{
  "version": "1.0.0",
  "mainModuleId": "main",
  "mainFunction": "initialize",
  "modules": {
    "core": {
      "url": "https://example.com/core.zipline",
      "sha256": "abc123...",
      "dependsOnIds": []
    },
    "utils": {
      "url": "https://example.com/utils.zipline",
      "sha256": "def456...",
      "dependsOnIds": ["core"]
    },
    "main": {
      "url": "https://example.com/main.zipline",
      "sha256": "ghi789...",
      "dependsOnIds": ["core", "utils"]
    }
  }
}
```

**Required Fields:**
- `version`: Semantic version of the manifest
- `mainModuleId`: ID of the main module to execute
- `mainFunction`: Name of the initialization function
- `modules`: Object mapping module IDs to metadata

**Module Metadata:**
- `url`: URL to download the .zipline file
- `sha256`: SHA256 hash for verification (64 hex characters)
- `dependsOnIds`: Array of module IDs this module depends on

## Event Types

The plugin emits events during the loading process:

| Event Type | Description | Fields |
|------------|-------------|--------|
| `manifest_downloading` | Manifest download started | `message` |
| `manifest_parsed` | Manifest successfully parsed | `message` |
| `module_downloading` | Module download started | `moduleName`, `message` |
| `module_loaded` | Module successfully loaded | `moduleName`, `message` |
| `extension_ready` | Extension fully loaded | `extensionId`, `message` |
| `error` | Operation failed | `error`, `message` |

## Error Handling

All async methods throw `PlatformException` on failure. Error codes include:

- `ManifestDownloadException` - Network error downloading manifest
- `ManifestParseException` - Invalid JSON or missing fields
- `CircularDependencyException` - Circular dependency detected
- `ModuleDownloadException` - Module download failed
- `ModuleVerificationException` - SHA256 hash mismatch
- `ModuleLoadException` - Zipline engine load failure
- `FunctionNotFoundException` - Function doesn't exist
- `FunctionExecutionException` - Function threw exception
- `ExtensionNotFoundException` - Extension ID not found

**Example:**
```dart
try {
  await plugin.loadExtension(manifestUrl);
} on PlatformException catch (e) {
  switch (e.code) {
    case 'ManifestDownloadException':
      print('Network error: ${e.message}');
      break;
    case 'ModuleVerificationException':
      print('Security error: ${e.message}');
      break;
    default:
      print('Error: ${e.message}');
  }
}
```

## Caching

Downloaded modules are automatically cached in the app's internal storage:

```
<app_internal_storage>/zipline_cache/
  <manifest_version_1>/
    module_a.zipline
    module_b.zipline
  <manifest_version_2>/
    module_a.zipline
    module_c.zipline
```

**Benefits:**
- Faster subsequent loads (10x+ speedup)
- Offline operation after first load
- Automatic cache invalidation on version change
- SHA256 verification on cache reads

## Security

### Module Verification

All modules are cryptographically verified using SHA256 hashes before execution. If a module's hash doesn't match the manifest, loading fails with `ModuleVerificationException`.

### Sandboxing

- Each extension runs in an isolated Zipline engine instance
- Extensions cannot access the device file system
- Extensions cannot make direct network requests
- Extensions cannot interfere with other extensions

### Best Practices

1. **Trust Your Sources**: Only load extensions from trusted manifest URLs
2. **HTTPS Only**: Always use HTTPS for manifest and module URLs
3. **Verify Hashes**: Ensure manifest SHA256 hashes are correct
4. **Handle Errors**: Always catch and handle `ModuleVerificationException`
5. **Update Regularly**: Keep the plugin updated for security patches

## Performance

### Benchmarks

Typical performance on a mid-range Android device:

- Manifest download and parse: < 5 seconds
- Module download (1MB): < 10 seconds
- Module loading: < 2 seconds
- Function call latency: < 100ms
- Cache hit speedup: > 10x faster than download

### Optimization Tips

1. **Use Caching**: Let the plugin cache modules for faster loads
2. **Minimize Dependencies**: Fewer modules = faster loading
3. **Optimize Module Size**: Smaller modules download faster
4. **Parallel Loading**: Load multiple extensions concurrently
5. **Preload Extensions**: Load extensions during app startup

## Troubleshooting

### Extension fails to load

**Problem**: `ManifestDownloadException` or `ModuleDownloadException`

**Solutions:**
- Check network connectivity
- Verify manifest URL is accessible
- Check for CORS issues (if using web server)
- Retry with exponential backoff

### SHA256 verification fails

**Problem**: `ModuleVerificationException`

**Solutions:**
- Verify SHA256 hashes in manifest are correct
- Clear cache and retry
- Check if module files were corrupted during upload
- Ensure manifest and modules are from the same version

### Function not found

**Problem**: `FunctionNotFoundException`

**Solutions:**
- Verify function name spelling
- Check that function is exported in the module
- Ensure all dependencies are loaded
- Review Zipline module compilation

### Out of memory

**Problem**: App crashes or slows down with many extensions

**Solutions:**
- Unload unused extensions
- Limit number of concurrent extensions
- Reduce module size
- Profile memory usage

## API Documentation

For detailed API documentation, see [API.md](API.md).

## Integration Guide

For a comprehensive integration guide, see [INTEGRATION_GUIDE.md](INTEGRATION_GUIDE.md).

## Example App

A complete example app is included in the `example/` directory. To run it:

```bash
cd example
flutter run
```

The example demonstrates:
- Loading a Zipline extension
- Monitoring loading progress with events
- Calling functions with arguments
- Displaying results in a UI
- Error handling and recovery
- Unloading extensions

## Contributing

Contributions are welcome! Please read our [Contributing Guide](CONTRIBUTING.md) before submitting PRs.

### Development Setup

1. Clone the repository
2. Install dependencies: `flutter pub get`
3. Run tests: `flutter test`
4. Run example: `cd example && flutter run`

### Running Tests

```bash
# Dart tests
flutter test

# Kotlin tests
cd android
./gradlew test

# Integration tests
cd example
flutter test integration_test/
```

## Roadmap

### Version 1.x
- ✅ Android support
- ✅ SHA256 verification
- ✅ Module caching
- ✅ Event streaming
- ✅ Concurrent extensions

### Version 2.0 (Planned)
- iOS support using Kotlin/Native
- WebAssembly support
- Manifest signing
- Incremental updates
- Hot reload support
- Debugging tools

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- [Cash App Zipline](https://github.com/cashapp/zipline) - The underlying dynamic code execution engine
- [Flutter](https://flutter.dev) - The UI framework
- [OkHttp](https://square.github.io/okhttp/) - HTTP client library

## Support

- 📖 [Documentation](API.md)
- 💬 [Discussions](https://github.com/yourusername/flutter_zipline_plugin/discussions)
- 🐛 [Issue Tracker](https://github.com/yourusername/flutter_zipline_plugin/issues)
- 📧 [Email Support](mailto:support@example.com)

## Related Projects

- [Soundbound](https://soundbound.app) - Music streaming app using this plugin
- [Zipline](https://github.com/cashapp/zipline) - Cash App's Zipline engine
- [QuickJS](https://bellard.org/quickjs/) - JavaScript engine used by Zipline

---

Made with ❤️ by the Flutter community
