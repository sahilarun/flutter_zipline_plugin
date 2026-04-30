import 'dart:async';

import 'package:flutter/services.dart';

import 'models/zipline_event.dart';

class ZiplinePlugin {
  static const MethodChannel _methodChannel =
      MethodChannel('com.zipline/methods');

  static const EventChannel _eventChannel =
      EventChannel('com.zipline/events');

  Stream<ZiplineEvent>? _eventStream;

  Stream<ZiplineEvent> get events {
    _eventStream ??= _eventChannel
        .receiveBroadcastStream()
        .map((dynamic event) => ZiplineEvent.fromMap(
              Map<String, dynamic>.from(event as Map),
            ));
    return _eventStream!;
  }

  Future<String> loadExtension(String manifestUrl) async {
    try {
      final result = await _methodChannel.invokeMethod<String>(
        'loadExtension',
        {'manifestUrl': manifestUrl},
      );
      if (result == null) {
        throw PlatformException(
          code: 'NULL_RESULT',
          message: 'loadExtension returned null',
        );
      }
      return result;
    } on PlatformException catch (e) {
      throw PlatformException(
        code: e.code,
        message: e.message ?? 'Unknown error during loadExtension',
        details: e.details,
      );
    }
  }

  Future<dynamic> callFunction(
    String extensionId,
    String functionName,
    Map<String, dynamic> arguments,
  ) async {
    try {
      final result = await _methodChannel.invokeMethod<dynamic>(
        'callFunction',
        {
          'extensionId': extensionId,
          'functionName': functionName,
          'arguments': arguments,
        },
      );
      return result;
    } on PlatformException catch (e) {
      throw PlatformException(
        code: e.code,
        message: e.message ?? 'Unknown error during callFunction',
        details: e.details,
      );
    }
  }

  Future<void> unloadExtension(String extensionId) async {
    try {
      await _methodChannel.invokeMethod<void>(
        'unloadExtension',
        {'extensionId': extensionId},
      );
    } on PlatformException catch (e) {
      throw PlatformException(
        code: e.code,
        message: e.message ?? 'Unknown error during unloadExtension',
        details: e.details,
      );
    }
  }
}
