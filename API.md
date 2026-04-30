# Flutter Zipline Plugin API Documentation

## Overview

The Flutter Zipline Plugin provides a clean, type-safe Dart API for dynamically loading and executing Zipline bytecode modules at runtime. This document describes all public APIs, their parameters, return values, and error conditions.

## Table of Contents

- [Core Classes](#core-classes)
  - [ZiplinePlugin](#ziplineplugin)
  - [ZiplineEvent](#ziplineevent)
  - [ExtensionState](#extensionstate)
- [Usage Patterns](#usage-patterns)
- [Error Handling](#error-handling)
- [Platform Channel Protocol](#platform-channel-protocol)

---

## Core Classes

### ZiplinePlugin

The main entry point for the Flutter Zipline Plugin. Provides methods to load, call functions in, and unload Zipline extensions.

#### Constructor

```dart
ZiplinePlugin()
```

Creates a new instance of the plugin. Multiple instances can be created, but they all share the same underlying platform channel.

#### Properties

##### events

```dart
Stream<ZiplineEvent> get events
```

A broadcast stream of lifecycle events emitted during extension loading and execution.

**Event Types:**
- `manifest_downloading`: Manifest download started
- `manifest_parsed`: Manifest successfully parsed
- `module_downloading`: Module download started (includes `moduleName`)
- `module_loaded`: Module successfully loaded (includes `moduleName`)
- `extension_ready`: Extension fully loaded and ready (includes `extensionId`)
- `error`: Operation failed (includes `error` message)

**Example:**
```dart
final plugin = ZiplinePlugin();

plugin.events.listen((event) {
  switch (event.type) {
    case 'manifest_downloading':
      print('Downloading manifest...');
      break;
    case 'module_downloading':
      print('Downloading module: ${event.moduleName}');
      break;
    case 'extension_ready':
      print('Extension ready: ${event.extensionId}');
      break;
    case 'error':
      print('Error: ${event.error}');
      break;
  }
});
```

#### Methods

##### loadExtension

```dart
Future<String> loadExtension(String manifestUrl)
```

Loads an extension from the specified manifest URL.

**Parameters:**
- `manifestUrl` (String): The URL of the manifest.zipline.json file

**Returns:**
- `Future<String>`: A unique extension ID that can be used for subsequent operations

**Throws:**
- `PlatformException` with code:
  - `ManifestDownloadException`: Manifest URL is unreachable or returns HTTP error
  - `ManifestParseException`: Manifest contains invalid JSON or missing required fields
  - `CircularDependencyException`: Module dependency graph contains a cycle
  - `ModuleDownloadException`: One or more modules failed to download
  - `ModuleVerificationException`: Module SHA256 hash doesn't match manifest
  - `ModuleLoadException`: Zipline engine failed to load a module
  - `FunctionExecutionException`: Main function failed to execute

**Workflow:**
1. Downloads the manifest file from the provided URL
2. Parses the manifest and validates required fields
3. Resolves module dependencies in topological order
4. Downloads modules (or retrieves from cache)
5. Verifies each module's SHA256 hash
6. Loads modules into a new Zipline engine instance
7. Executes the main function specified in the manifest
8. Returns a unique extension ID

**Example:**
```dart
try {
  final extensionId = await plugin.loadExtension(
    'https://example.com/manifest.zipline.json'
  );
  print('Extension loaded: $extensionId');
} on PlatformException catch (e) {
  print('Failed to load: ${e.code} - ${e.message}');
}
```

##### callFunction

```dart
Future<dynamic> callFunction(
  String extensionId,
  String functionName,
  Map<String, dynamic> arguments,
)
```

Calls a function in a loaded extension.

**Parameters:**
- `extensionId` (String): The extension ID returned by `loadExtension`
- `functionName` (String): The name of the function to call
- `arguments` (Map<String, dynamic>): Arguments to pass to the function as a JSON object

**Returns:**
- `Future<dynamic>`: The result returned by the function (can be any JSON-serializable type)

**Throws:**
- `PlatformException` with code:
  - `ExtensionNotFoundException`: The extension ID is not found or has been unloaded
  - `FunctionNotFoundException`: The function does not exist in the extension
  - `FunctionExecutionException`: The function threw an exception during execution

**Argument Serialization:**
- Arguments are serialized to JSON before being passed to the native layer
- Supports: strings, numbers, booleans, null, nested objects, and arrays
- Unsupported types (functions, symbols, etc.) will cause serialization errors

**Result Deserialization:**
- Results are deserialized from JSON
- The return type is `dynamic` - cast to the expected type in your code
- Complex objects are returned as `Map<String, dynamic>` or `List<dynamic>`

**Example:**
```dart
try {
  final result = await plugin.callFunction(
    extensionId,
    'search',
    {
      'query': 'flutter',
      'limit': 10,
      'filters': {
        'type': 'video',
        'duration': 'short',
      },
    },
  );
  
  // Cast result to expected type
  final items = (result as Map)['items'] as List;
  print('Found ${items.length} results');
} on PlatformException catch (e) {
  print('Function call failed: ${e.code} - ${e.message}');
}
```

##### unloadExtension

```dart
Future<void> unloadExtension(String extensionId)
```

Unloads an extension and releases its resources.

**Parameters:**
- `extensionId` (String): The extension ID returned by `loadExtension`

**Returns:**
- `Future<void>`: Completes when the extension is unloaded

**Throws:**
- `PlatformException` with code:
  - `ExtensionNotFoundException`: The extension ID is not found

**Behavior:**
- Terminates the Zipline engine instance
- Releases all loaded modules from memory
- Invalidates the extension ID (cannot be used for future calls)
- Does not affect other loaded extensions

**Example:**
```dart
try {
  await plugin.unloadExtension(extensionId);
  print('Extension unloaded successfully');
} on PlatformException catch (e) {
  print('Failed to unload: ${e.code} - ${e.message}');
}
```

---

### ZiplineEvent

Represents an event emitted during the extension lifecycle.

#### Properties

```dart
class ZiplineEvent {
  final String type;
  final String? extensionId;
  final String? moduleName;
  final String? message;
  final String? error;
}
```

**Fields:**
- `type` (String): The event type (see event types above)
- `extensionId` (String?): The extension ID (present in `extension_ready` events)
- `moduleName` (String?): The module name (present in `module_downloading` and `module_loaded` events)
- `message` (String?): A descriptive message about the event
- `error` (String?): Error details (present only when `type` is `error`)

#### Constructors

##### ZiplineEvent

```dart
ZiplineEvent({
  required String type,
  String? extensionId,
  String? moduleName,
  String? message,
  String? error,
})
```

Creates a new event instance.

##### ZiplineEvent.fromMap

```dart
factory ZiplineEvent.fromMap(Map<String, dynamic> map)
```

Creates an event from a map received from the platform channel. Used internally by the plugin.

#### Methods

##### toMap

```dart
Map<String, dynamic> toMap()
```

Converts the event to a map for serialization.

##### toString

```dart
String toString()
```

Returns a string representation of the event.

**Example:**
```dart
final event = ZiplineEvent(
  type: 'module_loaded',
  moduleName: 'core',
  message: 'Core module loaded successfully',
);

print(event); // ZiplineEvent(type: module_loaded, ...)
```

---

### ExtensionState

Represents the current state of a loaded extension.

#### Properties

```dart
class ExtensionState {
  final String id;
  final String manifestUrl;
  final String version;
  final ExtensionStatus status;
}
```

**Fields:**
- `id` (String): The unique extension ID
- `manifestUrl` (String): The manifest URL from which the extension was loaded
- `version` (String): The version from the manifest
- `status` (ExtensionStatus): The current status

#### ExtensionStatus Enum

```dart
enum ExtensionStatus {
  loading,   // Extension is being loaded
  ready,     // Extension is fully loaded and ready
  error,     // Extension encountered an error
  unloaded,  // Extension has been unloaded
}
```

#### Constructors

##### ExtensionState

```dart
ExtensionState({
  required String id,
  required String manifestUrl,
  required String version,
  required ExtensionStatus status,
})
```

Creates a new extension state instance.

##### ExtensionState.fromMap

```dart
factory ExtensionState.fromMap(Map<String, dynamic> map)
```

Creates an extension state from a map received from the platform channel.

#### Methods

##### toMap

```dart
Map<String, dynamic> toMap()
```

Converts the extension state to a map for serialization.

**Example:**
```dart
final state = ExtensionState(
  id: 'abc-123',
  manifestUrl: 'https://example.com/manifest.zipline.json',
  version: '1.0.0',
  status: ExtensionStatus.ready,
);

print('Extension ${state.id} is ${state.status}');
```

---

## Usage Patterns

### Basic Extension Loading

```dart
import 'package:flutter_zipline_plugin/flutter_zipline_plugin.dart';

class MyExtensionManager {
  final _plugin = ZiplinePlugin();
  String? _extensionId;
  
  Future<void> loadExtension() async {
    try {
      _extensionId = await _plugin.loadExtension(
        'https://example.com/manifest.zipline.json'
      );
      print('Loaded: $_extensionId');
    } catch (e) {
      print('Failed to load: $e');
    }
  }
  
  Future<void> callSearch(String query) async {
    if (_extensionId == null) {
      throw Exception('Extension not loaded');
    }
    
    final result = await _plugin.callFunction(
      _extensionId!,
      'search',
      {'query': query},
    );
    
    return result;
  }
  
  Future<void> unload() async {
    if (_extensionId != null) {
      await _plugin.unloadExtension(_extensionId!);
      _extensionId = null;
    }
  }
}
```

### Progress Monitoring

```dart
class ExtensionLoader {
  final _plugin = ZiplinePlugin();
  final _loadingProgress = <String>[];
  
  void startListening() {
    _plugin.events.listen((event) {
      switch (event.type) {
        case 'manifest_downloading':
          _loadingProgress.add('Downloading manifest...');
          break;
        case 'manifest_parsed':
          _loadingProgress.add('Manifest parsed');
          break;
        case 'module_downloading':
          _loadingProgress.add('Downloading ${event.moduleName}...');
          break;
        case 'module_loaded':
          _loadingProgress.add('Loaded ${event.moduleName}');
          break;
        case 'extension_ready':
          _loadingProgress.add('Extension ready!');
          break;
        case 'error':
          _loadingProgress.add('Error: ${event.error}');
          break;
      }
      
      // Update UI with progress
      notifyListeners();
    });
  }
}
```

### Multiple Extensions

```dart
class MultiExtensionManager {
  final _plugin = ZiplinePlugin();
  final _extensions = <String, String>{}; // name -> extensionId
  
  Future<void> loadExtension(String name, String manifestUrl) async {
    final extensionId = await _plugin.loadExtension(manifestUrl);
    _extensions[name] = extensionId;
  }
  
  Future<dynamic> callFunction(
    String extensionName,
    String functionName,
    Map<String, dynamic> args,
  ) async {
    final extensionId = _extensions[extensionName];
    if (extensionId == null) {
      throw Exception('Extension $extensionName not loaded');
    }
    
    return await _plugin.callFunction(extensionId, functionName, args);
  }
  
  Future<void> unloadAll() async {
    for (final extensionId in _extensions.values) {
      await _plugin.unloadExtension(extensionId);
    }
    _extensions.clear();
  }
}
```

### Error Recovery

```dart
class RobustExtensionLoader {
  final _plugin = ZiplinePlugin();
  
  Future<String?> loadWithRetry(
    String manifestUrl, {
    int maxRetries = 3,
    Duration retryDelay = const Duration(seconds: 2),
  }) async {
    for (var attempt = 0; attempt < maxRetries; attempt++) {
      try {
        return await _plugin.loadExtension(manifestUrl);
      } on PlatformException catch (e) {
        if (e.code == 'ModuleVerificationException') {
          // Verification failed - don't retry
          rethrow;
        }
        
        if (attempt == maxRetries - 1) {
          // Last attempt failed
          rethrow;
        }
        
        // Wait before retrying
        await Future.delayed(retryDelay);
      }
    }
    
    return null;
  }
}
```

---

## Error Handling

### Error Codes

The plugin throws `PlatformException` with the following error codes:

| Error Code | Description | Retry Recommended |
|------------|-------------|-------------------|
| `ManifestDownloadException` | Failed to download manifest (network error, HTTP error) | Yes |
| `ManifestParseException` | Invalid JSON or missing required fields in manifest | No |
| `CircularDependencyException` | Module dependency graph contains a cycle | No |
| `ModuleDownloadException` | Failed to download one or more modules | Yes |
| `ModuleVerificationException` | Module SHA256 hash doesn't match manifest | No (security issue) |
| `ModuleLoadException` | Zipline engine failed to load a module | No |
| `FunctionNotFoundException` | Requested function doesn't exist in extension | No |
| `FunctionExecutionException` | Function threw an exception during execution | Depends on error |
| `ExtensionNotFoundException` | Extension ID not found or already unloaded | No |

### Error Message Format

Error messages include:
- **Operation type**: What operation failed (download, parse, verify, load, call)
- **Context**: Relevant details (URL, module name, function name, HTTP status code)
- **Cause**: The underlying error message

**Example Error Messages:**
```
ManifestDownloadException: Failed to download manifest: HTTP 404
ModuleVerificationException: SHA256 mismatch for module 'core': expected abc123..., got def456...
FunctionNotFoundException: Function 'searchVideos' not found in extension abc-123
```

### Best Practices

1. **Always handle PlatformException**: All async methods can throw exceptions
2. **Check error codes**: Use error codes to determine if retry is appropriate
3. **Log errors**: Include error code and message in logs for debugging
4. **Provide user feedback**: Show meaningful error messages to users
5. **Clean up on error**: Unload partially loaded extensions if needed

**Example:**
```dart
try {
  final extensionId = await plugin.loadExtension(manifestUrl);
  // Use extension...
} on PlatformException catch (e) {
  // Log the error
  logger.error('Extension load failed', error: e, code: e.code);
  
  // Determine if retry is appropriate
  final shouldRetry = e.code == 'ManifestDownloadException' ||
                      e.code == 'ModuleDownloadException';
  
  // Show user-friendly message
  if (shouldRetry) {
    showError('Network error. Please check your connection and try again.');
  } else {
    showError('Failed to load extension: ${e.message}');
  }
}
```

---

## Platform Channel Protocol

### Method Channel: `com.zipline/methods`

The plugin uses a MethodChannel for invoking platform-specific operations.

#### loadExtension

**Request:**
```json
{
  "method": "loadExtension",
  "arguments": {
    "manifestUrl": "https://example.com/manifest.zipline.json"
  }
}
```

**Response:**
- Success: `String` (extension ID)
- Error: `PlatformException` with error code and message

#### callFunction

**Request:**
```json
{
  "method": "callFunction",
  "arguments": {
    "extensionId": "abc-123",
    "functionName": "search",
    "arguments": {
      "query": "test",
      "limit": 10
    }
  }
}
```

**Response:**
- Success: `dynamic` (function result)
- Error: `PlatformException` with error code and message

#### unloadExtension

**Request:**
```json
{
  "method": "unloadExtension",
  "arguments": {
    "extensionId": "abc-123"
  }
}
```

**Response:**
- Success: `null`
- Error: `PlatformException` with error code and message

### Event Channel: `com.zipline/events`

The plugin uses an EventChannel for streaming lifecycle events.

**Event Format:**
```json
{
  "type": "manifest_downloading|manifest_parsed|module_downloading|module_loaded|extension_ready|error",
  "extensionId": "abc-123",
  "moduleName": "core",
  "message": "Descriptive message",
  "error": "Error details (only if type is 'error')"
}
```

**Event Flow:**
1. `manifest_downloading` → Manifest download started
2. `manifest_parsed` → Manifest successfully parsed
3. `module_downloading` (for each module) → Module download started
4. `module_loaded` (for each module) → Module successfully loaded
5. `extension_ready` → Extension fully loaded and ready
6. `error` (if any operation fails) → Operation failed

---

## Version Compatibility

- **Flutter**: 3.0.0 or higher
- **Dart**: 3.0.0 or higher
- **Android**: API level 21 (Android 5.0) or higher
- **Zipline**: 1.x (embedded in plugin)

## Thread Safety

- All plugin methods are thread-safe and can be called from any isolate
- The event stream is a broadcast stream and can have multiple listeners
- Multiple extensions can be loaded and used concurrently without conflicts

## Performance Considerations

- **Caching**: Downloaded modules are cached locally for faster subsequent loads
- **Parallel Downloads**: Independent modules are downloaded in parallel
- **Memory**: Each extension maintains its own Zipline engine instance
- **Function Calls**: Function call latency is typically < 100ms

## Security

- **SHA256 Verification**: All modules are cryptographically verified before execution
- **Sandboxing**: Extensions run in isolated Zipline engine instances
- **No File System Access**: Extensions cannot access the device file system
- **No Network Access**: Extensions cannot make direct network requests

---

## See Also

- [README.md](README.md) - Plugin overview and quick start
- [INTEGRATION_GUIDE.md](INTEGRATION_GUIDE.md) - Detailed integration guide
- [CHANGELOG.md](CHANGELOG.md) - Version history
- [Example App](example/) - Complete working example
