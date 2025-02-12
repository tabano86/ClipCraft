# FAQ

**Q:** How do I enable concurrency?  
**A:** In settings, pick either “Thread Pool” or “Coroutines” under “Concurrency Mode.” Then specify max tasks if needed.

**Q:** Does it support large codebases?  
**A:** Yes. Use chunking plus concurrency for optimal results.

**Q:** Can I skip binary files or certain folders?  
**A:** Yes. Turn on binary detection, define custom ignore patterns, or rely on `.gitignore`.

**Q:** Does it remove wildcard imports?  
**A:** Spotless doesn’t automatically remove wildcard imports by default. You can try using a different code formatter or rely on IDE settings to avoid wildcard imports.

**Q:** How do I reset to defaults?  
**A:** Run “ClipCraft Reset Defaults” from the ClipCraft submenu, or manually delete/replace the default profile in the plugin settings.
