# Flutter Zipline Plugin Example

A comprehensive example app demonstrating how to use the Flutter Zipline Plugin to dynamically load and execute Zipline extensions at runtime.

## Features

This example app demonstrates:

- **Extension Loading**: Load Zipline extensions from a manifest URL
- **Lifecycle Events**: Monitor the loading process with real-time event updates
- **Function Invocation**: Call functions in loaded extensions with arguments
- **Search UI**: Interactive search interface to test extension functionality
- **Error Handling**: Graceful error handling with user-friendly messages
- **Extension Unloading**: Clean up resources by unloading extensions

## What This Example Shows

### Extension Control
- Load the Soundbound YouTube extension from a manifest URL
- View the extension ID and loading status
- Unload the extension to free resources

### Search Functionality
- Enter search queries to test the extension's search function
- View formatted search results with titles, descriptions, and metadata
- Handle search errors gracefully

### Event Monitoring
- Real-time event stream showing:
  - Manifest downloading
  - Manifest parsed
  - Module downloading (per module)
  - Module loaded (per module)
  - Extension ready
  - Error events with details

## Running the Example

### Prerequisites

- Flutter 3.x or higher
- Dart 3.x or higher
- Android device or emulator (API 21+)

### Steps

1. Navigate to the example directory:
   ```bash
   cd flutter_zipline_plugin/example
   ```

2. Get dependencies:
   ```bash
   flutter pub get
   ```

3. Run the app:
   ```bash
   flutter run
   ```

## Using the Example App

### 1. Load an Extension

Tap the **"Load Extension"** button to download and load the Soundbound YouTube extension. The app will:
- Download the manifest from the configured URL
- Parse the manifest and resolve dependencies
- Download and verify all required modules
- Load modules into the Zipline engine
- Execute the main initialization function

Watch the **Events** tab to see the loading progress in real-time.

### 2. Search for Content

Once the extension is loaded:
1. Enter a search query in the text field (e.g., "music", "tutorial")
2. Tap the **"Search"** button or press Enter
3. View the results in the **Results** tab

Each result shows:
- Title
- Description (if available)
- Duration or other metadata (if available)

### 3. Unload the Extension

Tap the **"Unload"** button to:
- Terminate the extension instance
- Release Zipline engine resources
- Clear search results and reset the UI

You can then load the extension again or load a different extension.

## Code Structure

### ExtensionManager Class

The `ExtensionManager` class demonstrates best practices for using the plugin:

```dart
class ExtensionManager {
  final ZiplinePlugin _plugin = ZiplinePlugin();
  String? _extensionId;

  // Listen to lifecycle events
  Stream<ZiplineEvent> get events => _plugin.events;

  // Load an extension
  Future<String> loadYouTubeExtension() async {
    _extensionId = await _plugin.loadExtension(manifestUrl);
    return _extensionId!;
  }

  // Call a function in the extension
  Future<List<dynamic>> search(String query) async {
    final result = await _plugin.callFunction(
      _extensionId!,
      'search',
      {'query': query, 'limit': 20},
    );
    return result['items'] as List<dynamic>;
  }

  // Unload the extension
  Future<void> unload() async {
    await _plugin.unloadExtension(_extensionId!);
    _extensionId = null;
  }
}
```

### UI Components

- **Extension Control Card**: Load/unload buttons and status display
- **Search Card**: Query input and search button
- **Tabbed Results View**: 
  - Results tab: Formatted search results
  - Events tab: Real-time lifecycle events

## Configuration

### Manifest URL

The example uses the Soundbound YouTube extension manifest URL:

```dart
const manifestUrl = 'https://soundbound.app/extensions/youtube/manifest.zipline.json';
```

To use a different extension, modify the `manifestUrl` in the `loadYouTubeExtension()` method.

## Error Handling

The example demonstrates comprehensive error handling:

- **Network Errors**: Shows user-friendly messages for connection failures
- **Parse Errors**: Displays manifest parsing errors
- **Verification Errors**: Reports SHA256 hash mismatches
- **Function Errors**: Handles exceptions from extension functions
- **UI Feedback**: Uses SnackBars for error notifications

## Testing

### Manual Testing Checklist

- [ ] Load extension successfully
- [ ] View loading events in real-time
- [ ] Perform search with valid query
- [ ] View formatted search results
- [ ] Handle search with empty query
- [ ] Handle search before loading extension
- [ ] Unload extension successfully
- [ ] Load extension again after unloading
- [ ] Test with network disconnected (should show error)
- [ ] Test with invalid manifest URL (should show error)

## Troubleshooting

### Extension fails to load

- Check your internet connection
- Verify the manifest URL is correct and accessible
- Check the Events tab for specific error messages
- Ensure your device/emulator has API level 21 or higher

### Search returns no results

- Verify the extension loaded successfully (check status)
- Try a different search query
- Check the Events tab for error events
- Ensure the extension's search function is implemented correctly

### App crashes on load

- Check Android logs: `flutter logs`
- Verify Zipline dependencies are correctly configured
- Ensure the manifest format is valid JSON
- Check that all module URLs are accessible

## Learn More

- [Flutter Zipline Plugin Documentation](../README.md)
- [Zipline by Cash App](https://github.com/cashapp/zipline)
- [Flutter Platform Channels](https://docs.flutter.dev/platform-integration/platform-channels)

## License

This example app is part of the Flutter Zipline Plugin package and is licensed under the same terms.
