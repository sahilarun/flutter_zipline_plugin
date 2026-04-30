import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter_zipline_plugin/flutter_zipline_plugin.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter Zipline Plugin Example',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
        useMaterial3: true,
      ),
      home: const ExtensionDemoPage(),
    );
  }
}

class ExtensionDemoPage extends StatefulWidget {
  const ExtensionDemoPage({super.key});

  @override
  State<ExtensionDemoPage> createState() => _ExtensionDemoPageState();
}

class _ExtensionDemoPageState extends State<ExtensionDemoPage> {
  final _extensionManager = ExtensionManager();
  final _searchController = TextEditingController();
  
  bool _isLoading = false;
  bool _isSearching = false;
  String? _extensionId;
  List<dynamic> _searchResults = [];
  final List<String> _events = [];

  @override
  void initState() {
    super.initState();
    _listenToEvents();
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  void _listenToEvents() {
    _extensionManager.events.listen((event) {
      setState(() {
        final eventText = '${event.type}: ${event.message ?? event.moduleName ?? ''}';
        _events.insert(0, eventText);
        if (_events.length > 20) {
          _events.removeLast();
        }
      });
    });
  }

  Future<void> _loadExtension() async {
    setState(() {
      _isLoading = true;
      _searchResults = [];
    });

    try {
      final extensionId = await _extensionManager.loadYouTubeExtension();
      setState(() {
        _extensionId = extensionId;
        _isLoading = false;
      });
      
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Extension loaded: ${extensionId.substring(0, 8)}...'),
            backgroundColor: Colors.green,
          ),
        );
      }
    } catch (e) {
      setState(() {
        _isLoading = false;
      });
      
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Failed to load extension: $e'),
            backgroundColor: Colors.red,
            duration: const Duration(seconds: 5),
          ),
        );
      }
    }
  }

  Future<void> _search() async {
    if (_extensionId == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Please load the extension first'),
          backgroundColor: Colors.orange,
        ),
      );
      return;
    }

    final query = _searchController.text.trim();
    if (query.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Please enter a search query'),
          backgroundColor: Colors.orange,
        ),
      );
      return;
    }

    setState(() {
      _isSearching = true;
      _searchResults = [];
    });

    try {
      final results = await _extensionManager.search(query);
      setState(() {
        _searchResults = results;
        _isSearching = false;
      });
    } catch (e) {
      setState(() {
        _isSearching = false;
      });
      
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Search failed: $e'),
            backgroundColor: Colors.red,
            duration: const Duration(seconds: 5),
          ),
        );
      }
    }
  }

  Future<void> _unloadExtension() async {
    if (_extensionId == null) return;

    try {
      await _extensionManager.unload();
      setState(() {
        _extensionId = null;
        _searchResults = [];
        _searchController.clear();
      });
      
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Extension unloaded'),
            backgroundColor: Colors.blue,
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Failed to unload extension: $e'),
            backgroundColor: Colors.red,
          ),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Zipline Plugin Demo'),
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
      ),
      body: Column(
        children: [
          // Extension Control Section
          Card(
            margin: const EdgeInsets.all(16),
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Text(
                    'Extension Control',
                    style: Theme.of(context).textTheme.titleLarge,
                  ),
                  const SizedBox(height: 8),
                  Text(
                    _extensionId == null
                        ? 'Status: Not loaded'
                        : 'Status: Loaded (${_extensionId!.substring(0, 8)}...)',
                    style: Theme.of(context).textTheme.bodyMedium,
                  ),
                  const SizedBox(height: 16),
                  Row(
                    children: [
                      Expanded(
                        child: ElevatedButton.icon(
                          onPressed: _isLoading || _extensionId != null
                              ? null
                              : _loadExtension,
                          icon: _isLoading
                              ? const SizedBox(
                                  width: 16,
                                  height: 16,
                                  child: CircularProgressIndicator(
                                    strokeWidth: 2,
                                  ),
                                )
                              : const Icon(Icons.download),
                          label: Text(_isLoading ? 'Loading...' : 'Load Extension'),
                        ),
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: ElevatedButton.icon(
                          onPressed: _extensionId != null ? _unloadExtension : null,
                          icon: const Icon(Icons.close),
                          label: const Text('Unload'),
                          style: ElevatedButton.styleFrom(
                            backgroundColor: Colors.red.shade100,
                          ),
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ),

          // Search Section
          Card(
            margin: const EdgeInsets.symmetric(horizontal: 16),
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Text(
                    'Search',
                    style: Theme.of(context).textTheme.titleLarge,
                  ),
                  const SizedBox(height: 8),
                  TextField(
                    controller: _searchController,
                    decoration: const InputDecoration(
                      hintText: 'Enter search query...',
                      border: OutlineInputBorder(),
                      prefixIcon: Icon(Icons.search),
                    ),
                    enabled: _extensionId != null && !_isSearching,
                    onSubmitted: (_) => _search(),
                  ),
                  const SizedBox(height: 8),
                  ElevatedButton.icon(
                    onPressed: _extensionId != null && !_isSearching
                        ? _search
                        : null,
                    icon: _isSearching
                        ? const SizedBox(
                            width: 16,
                            height: 16,
                            child: CircularProgressIndicator(
                              strokeWidth: 2,
                            ),
                          )
                        : const Icon(Icons.search),
                    label: Text(_isSearching ? 'Searching...' : 'Search'),
                  ),
                ],
              ),
            ),
          ),

          // Results Section
          Expanded(
            child: DefaultTabController(
              length: 2,
              child: Column(
                children: [
                  const TabBar(
                    tabs: [
                      Tab(text: 'Results', icon: Icon(Icons.list)),
                      Tab(text: 'Events', icon: Icon(Icons.event)),
                    ],
                  ),
                  Expanded(
                    child: TabBarView(
                      children: [
                        // Results Tab
                        _buildResultsTab(),
                        // Events Tab
                        _buildEventsTab(),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildResultsTab() {
    if (_searchResults.isEmpty) {
      return Center(
        child: Text(
          _extensionId == null
              ? 'Load an extension to start searching'
              : 'Enter a query and tap Search',
          style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                color: Colors.grey,
              ),
        ),
      );
    }

    return ListView.builder(
      padding: const EdgeInsets.all(16),
      itemCount: _searchResults.length,
      itemBuilder: (context, index) {
        final result = _searchResults[index];
        return Card(
          margin: const EdgeInsets.only(bottom: 8),
          child: ListTile(
            leading: CircleAvatar(
              child: Text('${index + 1}'),
            ),
            title: Text(
              result['title']?.toString() ?? 'No title',
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
            ),
            subtitle: result['description'] != null
                ? Text(
                    result['description'].toString(),
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                  )
                : null,
            trailing: result['duration'] != null
                ? Chip(
                    label: Text(result['duration'].toString()),
                    backgroundColor: Colors.blue.shade100,
                  )
                : null,
          ),
        );
      },
    );
  }

  Widget _buildEventsTab() {
    if (_events.isEmpty) {
      return Center(
        child: Text(
          'No events yet',
          style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                color: Colors.grey,
              ),
        ),
      );
    }

    return ListView.builder(
      padding: const EdgeInsets.all(16),
      itemCount: _events.length,
      itemBuilder: (context, index) {
        final event = _events[index];
        final isError = event.startsWith('error:');
        
        return Card(
          margin: const EdgeInsets.only(bottom: 4),
          color: isError ? Colors.red.shade50 : null,
          child: Padding(
            padding: const EdgeInsets.all(8),
            child: Row(
              children: [
                Icon(
                  isError ? Icons.error : Icons.info,
                  size: 16,
                  color: isError ? Colors.red : Colors.blue,
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    event,
                    style: const TextStyle(fontSize: 12),
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}

/// Manages the lifecycle of Zipline extensions.
///
/// This class demonstrates how to use the Flutter Zipline Plugin to:
/// - Load extensions from a manifest URL
/// - Listen to lifecycle events
/// - Call functions in loaded extensions
/// - Unload extensions to free resources
class ExtensionManager {
  final ZiplinePlugin _plugin = ZiplinePlugin();
  String? _extensionId;

  /// Stream of lifecycle events from the plugin.
  Stream<ZiplineEvent> get events => _plugin.events;

  /// Loads the Soundbound YouTube extension.
  ///
  /// Returns the extension ID on success.
  /// Throws an exception on failure.
  Future<String> loadYouTubeExtension() async {
    // Real Soundbound YouTube extension manifest URL (verified working)
    const manifestUrl = 'https://gitlab.com/shabinder/soundbound/-/raw/main/Experimental-Sources/in.shabinder.soundbound.extension.shabinder.youtube/43/manifest.zipline.json';
    
    _extensionId = await _plugin.loadExtension(manifestUrl);
    return _extensionId!;
  }

  /// Searches for content using the loaded extension.
  ///
  /// Returns a list of search results.
  /// Throws an exception if the extension is not loaded or the search fails.
  Future<List<dynamic>> search(String query) async {
    if (_extensionId == null) {
      throw Exception('Extension not loaded');
    }

    final result = await _plugin.callFunction(
      _extensionId!,
      'search',
      {'query': query, 'limit': 20},
    );

    // The result format depends on the extension implementation
    // This is a generic handler that works with various formats
    if (result is Map && result.containsKey('items')) {
      return result['items'] as List<dynamic>;
    } else if (result is List) {
      return result;
    } else {
      return [result];
    }
  }

  /// Unloads the currently loaded extension.
  ///
  /// Throws an exception if no extension is loaded or unload fails.
  Future<void> unload() async {
    if (_extensionId == null) {
      throw Exception('No extension loaded');
    }

    await _plugin.unloadExtension(_extensionId!);
    _extensionId = null;
  }
}
