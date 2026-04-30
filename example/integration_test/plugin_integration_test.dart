// This is a basic Flutter integration test.
//
// Since integration tests run in a full Flutter application, they can interact
// with the host side of a plugin implementation, unlike Dart unit tests.
//
// For more information about Flutter integration tests, please see
// https://flutter.dev/to/integration-testing

import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';

import 'package:flutter_zipline_plugin/flutter_zipline_plugin.dart';

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('ZiplinePlugin can be instantiated', (WidgetTester tester) async {
    final ZiplinePlugin plugin = ZiplinePlugin();
    expect(plugin, isNotNull);
    expect(plugin.events, isNotNull);
  });

  // Additional integration tests will be added once the native implementation is complete
}
