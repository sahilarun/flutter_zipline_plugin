/// Represents an event emitted during the extension lifecycle.
class ZiplineEvent {
  /// The type of event.
  final String type;

  /// The extension ID associated with this event (if applicable).
  final String? extensionId;

  /// The module name associated with this event (if applicable).
  final String? moduleName;

  /// A descriptive message about the event.
  final String? message;

  /// Error details if the event type is 'error'.
  final String? error;

  /// Creates a new [ZiplineEvent].
  ZiplineEvent({
    required this.type,
    this.extensionId,
    this.moduleName,
    this.message,
    this.error,
  });

  factory ZiplineEvent.fromMap(Map<String, dynamic> map) {
    return ZiplineEvent(
      type: map['type'] as String,
      extensionId: map['extensionId'] as String?,
      moduleName: map['moduleName'] as String?,
      message: map['message'] as String?,
      error: map['error'] as String?,
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'type': type,
      if (extensionId != null) 'extensionId': extensionId,
      if (moduleName != null) 'moduleName': moduleName,
      if (message != null) 'message': message,
      if (error != null) 'error': error,
    };
  }

  @override
  String toString() {
    return 'ZiplineEvent(type: $type, extensionId: $extensionId, moduleName: $moduleName, message: $message, error: $error)';
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;

    return other is ZiplineEvent &&
        other.type == type &&
        other.extensionId == extensionId &&
        other.moduleName == moduleName &&
        other.message == message &&
        other.error == error;
  }

  @override
  int get hashCode {
    return type.hashCode ^
        extensionId.hashCode ^
        moduleName.hashCode ^
        message.hashCode ^
        error.hashCode;
  }
}
