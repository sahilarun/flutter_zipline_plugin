import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_zipline_plugin/flutter_zipline_plugin.dart';

void main() {
  group('ZiplinePlugin', () {
    test('can be instantiated', () {
      final plugin = ZiplinePlugin();
      expect(plugin, isNotNull);
    });

    test('events stream is not null', () {
      final plugin = ZiplinePlugin();
      expect(plugin.events, isNotNull);
    });
  });

  group('ZiplineEvent', () {
    test('fromMap creates correct event', () {
      final map = {
        'type': 'manifest_downloading',
        'extensionId': 'test-id',
        'moduleName': 'test-module',
        'message': 'test message',
        'error': null,
      };

      final event = ZiplineEvent.fromMap(map);

      expect(event.type, 'manifest_downloading');
      expect(event.extensionId, 'test-id');
      expect(event.moduleName, 'test-module');
      expect(event.message, 'test message');
      expect(event.error, null);
    });

    test('toMap creates correct map', () {
      final event = ZiplineEvent(
        type: 'module_loaded',
        extensionId: 'test-id',
        moduleName: 'test-module',
        message: 'test message',
      );

      final map = event.toMap();

      expect(map['type'], 'module_loaded');
      expect(map['extensionId'], 'test-id');
      expect(map['moduleName'], 'test-module');
      expect(map['message'], 'test message');
      expect(map.containsKey('error'), false);
    });

    test('equality works correctly', () {
      final event1 = ZiplineEvent(
        type: 'extension_ready',
        extensionId: 'test-id',
      );
      final event2 = ZiplineEvent(
        type: 'extension_ready',
        extensionId: 'test-id',
      );
      final event3 = ZiplineEvent(
        type: 'error',
        extensionId: 'test-id',
      );

      expect(event1, equals(event2));
      expect(event1, isNot(equals(event3)));
    });
  });

  group('ExtensionState', () {
    test('fromMap creates correct state', () {
      final map = {
        'id': 'test-id',
        'manifestUrl': 'https://example.com/manifest.json',
        'version': '1.0.0',
        'status': 'ready',
      };

      final state = ExtensionState.fromMap(map);

      expect(state.id, 'test-id');
      expect(state.manifestUrl, 'https://example.com/manifest.json');
      expect(state.version, '1.0.0');
      expect(state.status, ExtensionStatus.ready);
    });

    test('toMap creates correct map', () {
      final state = ExtensionState(
        id: 'test-id',
        manifestUrl: 'https://example.com/manifest.json',
        version: '1.0.0',
        status: ExtensionStatus.loading,
      );

      final map = state.toMap();

      expect(map['id'], 'test-id');
      expect(map['manifestUrl'], 'https://example.com/manifest.json');
      expect(map['version'], '1.0.0');
      expect(map['status'], 'loading');
    });

    test('equality works correctly', () {
      final state1 = ExtensionState(
        id: 'test-id',
        manifestUrl: 'https://example.com/manifest.json',
        version: '1.0.0',
        status: ExtensionStatus.ready,
      );
      final state2 = ExtensionState(
        id: 'test-id',
        manifestUrl: 'https://example.com/manifest.json',
        version: '1.0.0',
        status: ExtensionStatus.ready,
      );
      final state3 = ExtensionState(
        id: 'test-id',
        manifestUrl: 'https://example.com/manifest.json',
        version: '1.0.0',
        status: ExtensionStatus.error,
      );

      expect(state1, equals(state2));
      expect(state1, isNot(equals(state3)));
    });

    test('all ExtensionStatus values are accessible', () {
      expect(ExtensionStatus.loading, isNotNull);
      expect(ExtensionStatus.ready, isNotNull);
      expect(ExtensionStatus.error, isNotNull);
      expect(ExtensionStatus.unloaded, isNotNull);
    });
  });
}
