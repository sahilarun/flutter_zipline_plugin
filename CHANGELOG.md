# Changelog

All notable changes to the Flutter Zipline Plugin will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2024-01-15

### Added

#### Core Features
- **Dynamic Extension Loading**: Load Zipline bytecode modules from remote manifest URLs
- **Cryptographic Verification**: SHA256 hash verification for all downloaded modules
- **Smart Caching**: Version-based local caching for offline operation and performance
- **Dependency Resolution**: Automatic topological sorting of module dependencies with cycle detection
- **Concurrent Extensions**: Support for loading and running multiple extensions simultaneously
- **Event Streaming**: Real-time lifecycle events during extension loading and execution
- **Sandboxed Execution**: Isolated Zipline engine instances for each extension

#### API
- `ZiplinePlugin` class with three main methods:
  - `loadExtension(String manifestUrl)`: Load an extension from a manifest URL
  - `callFunction(String extensionId, String functionName, Map<String, dynamic> arguments)`: Call functions in loaded extensions
  - `unloadExtension(String extensionId)`: Unload extensions and free resources
- `ZiplineEvent` model for lifecycle events with types:
  - `manifest_downloading`, `manifest_parsed`, `module_downloading`, `module_loaded`, `extension_ready`, `error`
- `ExtensionState` model for tracking extension status
- `ExtensionStatus` enum: `loading`, `ready`, `error`, `unloaded`

#### Platform Support
- Android API level 21+ (Android 5.0 Lollipop and above)
- Flutter 3.0.0+ compatibility
- Dart 3.0.0+ compatibility

#### Native Implementation (Android/Kotlin)
- `ZiplinePluginHandler`: Main platform channel handler
- `ManifestDownloader`: Downloads and parses manifest files with OkHttp
- `DependencyResolver`: Topological sorting with Kahn's algorithm
- `ModuleDownloader`: Downloads modules with SHA256 verification
- `ModuleCacheManager`: Version-based cache management
- `ZiplineEngineManager`: Manages Zipline engine instances and function invocation

#### Error Handling
- Comprehensive error types with descriptive messages:
  - `ManifestDownloadException`: Network errors downloading manifest
  - `ManifestParseException`: Invalid JSON or missing required fields
  - `CircularDependencyException`: Circular dependency detected in module graph
  - `ModuleDownloadException`: Module download failures
  - `ModuleVerificationException`: SHA256 hash mismatches
  - `ModuleLoadException`: Zipline engine load failures
  - `FunctionNotFoundException`: Function not found in extension
  - `FunctionExecutionException`: Function execution errors
  - `ExtensionNotFoundException`: Extension ID not found

#### Documentation
- Comprehensive API documentation (API.md)
- Detailed integration guide (INTEGRATION_GUIDE.md)
- Complete README with quick start and examples
- Inline dartdoc comments for all public APIs

#### Example Application
- Full-featured example app demonstrating:
  - Extension loading with progress tracking
  - Event monitoring and display
  - Function calls with argument passing
  - Search functionality with results display
  - Error handling and recovery
  - Extension unloading

#### Testing
- Unit tests for Dart API layer
- Unit tests for Kotlin native layer
- Property-based tests for core correctness properties
- Integration tests for end-to-end workflows
- Example app integration tests

### Technical Details

#### Dependencies
- `app.cash.zipline:zipline:1.x+` - Zipline engine for bytecode execution
- `com.squareup.okhttp3:okhttp:4.x+` - HTTP client for downloads
- `org.jetbrains.kotlinx:kotlinx-coroutines-android:1.x+` - Kotlin coroutines for async operations

#### Performance
- Manifest download and parse: < 5 seconds
- Module download (1MB): < 10 seconds
- Module loading: < 2 seconds
- Function call latency: < 100ms
- Cache hit speedup: > 10x faster than download

#### Security
- SHA256 verification for all modules before execution
- Cache integrity verification on retrieval
- Sandboxed execution (no file system or network access from extensions)
- HTTPS-only manifest and module URLs recommended
- Error message sanitization (no sensitive data exposure)

#### Cache Structure
```
<app_internal_storage>/zipline_cache/
  <manifest_version>/
    <module_id>.zipline
```

#### Manifest Format
```json
{
  "version": "1.0.0",
  "mainModuleId": "main",
  "mainFunction": "initialize",
  "modules": {
    "module_id": {
      "url": "https://example.com/module.zipline",
      "sha256": "64-character-hex-hash",
      "dependsOnIds": ["dependency1", "dependency2"]
    }
  }
}
```

### Platform Channels

#### Method Channel: `com.zipline/methods`
- `loadExtension`: Load extension from manifest URL
- `callFunction`: Invoke function in loaded extension
- `unloadExtension`: Unload extension and free resources

#### Event Channel: `com.zipline/events`
- Streams lifecycle events during extension loading
- Event types: manifest_downloading, manifest_parsed, module_downloading, module_loaded, extension_ready, error

### Known Limitations

- **Android Only**: iOS support planned for v2.0
- **No Hot Reload**: Extensions must be unloaded and reloaded for updates
- **No Debugging**: Debugging tools for extensions planned for v2.0
- **No Manifest Signing**: Cryptographic signing planned for v2.0

### Migration Guide

This is the initial release. No migration needed.

### Contributors

- Initial implementation and design
- Comprehensive testing and documentation
- Example application development

---

## [Unreleased]

### Planned for v1.1.0
- Performance improvements for large module graphs
- Enhanced error messages with recovery suggestions
- Manifest validation utilities
- Cache management APIs (clear cache, get cache size)
- Extension metadata queries

### Planned for v2.0.0
- iOS support using Kotlin/Native
- WebAssembly support (when Kotlin support stabilizes)
- Manifest signing for enhanced security
- Incremental module updates
- Hot reload support for extensions
- Debugging tools and profiler integration
- Extension sandboxing enhancements

---

## Version History

- **1.0.0** (2024-01-15) - Initial release with Android support
- **Unreleased** - Future enhancements

---

## Support

For issues, questions, or contributions:
- GitHub Issues: https://github.com/yourusername/flutter_zipline_plugin/issues
- Discussions: https://github.com/yourusername/flutter_zipline_plugin/discussions
- Email: support@example.com

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
