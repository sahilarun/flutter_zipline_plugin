import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'flutter_zipline_plugin_platform_interface.dart';

class MethodChannelFlutterZiplinePlugin extends FlutterZiplinePluginPlatform {
  @visibleForTesting
  final methodChannel = const MethodChannel('flutter_zipline_plugin');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>(
      'getPlatformVersion',
    );
    return version;
  }
}
