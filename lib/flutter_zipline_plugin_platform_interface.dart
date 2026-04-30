import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'flutter_zipline_plugin_method_channel.dart';

abstract class FlutterZiplinePluginPlatform extends PlatformInterface {
  FlutterZiplinePluginPlatform() : super(token: _token);

  static final Object _token = Object();

  static FlutterZiplinePluginPlatform _instance = MethodChannelFlutterZiplinePlugin();

  static FlutterZiplinePluginPlatform get instance => _instance;

  static set instance(FlutterZiplinePluginPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }
}
