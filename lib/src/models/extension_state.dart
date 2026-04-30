/// Represents the current state of a loaded extension.
class ExtensionState {
  /// The unique identifier for this extension instance.
  final String id;

  /// The manifest URL from which this extension was loaded.
  final String manifestUrl;

  /// The version of the extension from the manifest.
  final String version;

  /// The current status of the extension.
  final ExtensionStatus status;

  ExtensionState({
    required this.id,
    required this.manifestUrl,
    required this.version,
    required this.status,
  });

  factory ExtensionState.fromMap(Map<String, dynamic> map) {
    return ExtensionState(
      id: map['id'] as String,
      manifestUrl: map['manifestUrl'] as String,
      version: map['version'] as String,
      status: ExtensionStatus.values.firstWhere(
        (e) => e.name == map['status'],
        orElse: () => ExtensionStatus.error,
      ),
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'manifestUrl': manifestUrl,
      'version': version,
      'status': status.name,
    };
  }

  @override
  String toString() {
    return 'ExtensionState(id: $id, manifestUrl: $manifestUrl, version: $version, status: $status)';
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;

    return other is ExtensionState &&
        other.id == id &&
        other.manifestUrl == manifestUrl &&
        other.version == version &&
        other.status == status;
  }

  @override
  int get hashCode {
    return id.hashCode ^
        manifestUrl.hashCode ^
        version.hashCode ^
        status.hashCode;
  }
}

/// Represents the status of an extension.
enum ExtensionStatus {
  loading,
  ready,
  error,
  unloaded,
}
