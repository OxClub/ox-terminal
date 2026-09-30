# Package Repository Format

Nexus Terminal loads packages from HTTPS-hosted JSON repositories. This document describes the index format.

## Overview

A repository is a single JSON file containing a list of packages. The index is downloaded and cached locally; install/remove/update actions pull individual packages from the URLs specified in the index.

## Structure

```json
{
  "packages": [
    {
      "name": "package_name",
      "version": "1.0.0",
      "description": "Human-readable description",
      "category": "Category name",
      "url": "https://example.com/package.tar.gz",
      "sha256": "hexadecimal SHA-256 hash (optional but recommended)",
      "depends": ["dep1", "dep2"]
    }
  ]
}
```

## Fields

### Required

- **`name`** (string) – Unique package identifier. Used as the install key and directory name.
  - Format: lowercase alphanumerics, hyphens, underscores
  - Example: `python`, `git-core`, `user_tool`

- **`version`** (string) – Semantic version.
  - Format: numeric parts separated by dots (e.g., `3.11.2`, `1.0.0-alpha`)
  - Versions are compared numerically (so `1.10` > `1.9`)

- **`url`** (string) – HTTPS URL to the package archive.
  - Must end with `.zip`, `.tar.gz`, or `.tgz`
  - Used as-is; no redirect following by the app

### Recommended

- **`sha256`** (string) – Hexadecimal SHA-256 hash of the package file.
  - If provided, verified before extraction
  - If missing, package is used without verification (not recommended)

### Optional

- **`description`** (string) – Brief description shown in the UI.
- **`category`** (string) – Grouping for the UI. Predefined categories:
  - `Development`, `Networking`, `Utilities`, `Programming`, `Editors`, `System`, `Compression`, `Git`, `Languages`, `Database`, `Other`
  - If not in this list, shown as-is (but UI may default to `Other`)
- **`depends`** (array of strings) – List of other package names this package requires.
  - Resolved recursively before install
  - If a dependency is already installed, skipped

## Example Repository

```json
{
  "packages": [
    {
      "name": "python",
      "version": "3.11.2",
      "description": "Python 3 interpreter",
      "category": "Languages",
      "url": "https://myrepo.com/python-3.11.2.tar.gz",
      "sha256": "a1b2c3d4e5f6...",
      "depends": []
    },
    {
      "name": "git",
      "version": "2.40.1",
      "description": "Git version control",
      "category": "Git",
      "url": "https://myrepo.com/git-2.40.1.zip",
      "sha256": "f6e5d4c3b2a1...",
      "depends": ["openssl"]
    },
    {
      "name": "openssl",
      "version": "3.0.8",
      "description": "SSL/TLS library",
      "category": "Development",
      "url": "https://myrepo.com/openssl-3.0.8.tar.gz",
      "sha256": "1234567890ab...",
      "depends": []
    }
  ]
}
```

## Archive Format

Packages are extracted using standard tools:
- **`.zip`** files – unzipped as-is
- **`.tar.gz`** and **`.tgz`** files – decompressed and extracted

Extraction target: `$PREFIX` (app-private prefix directory).

### File Permissions

- Files with execute bit (`.tar.gz`) or in a `bin/` directory (`.zip`) are marked executable
- Symlinks in `.tar.gz` are preserved (if the system allows)
- Zip-slip attacks (paths like `../../../etc/passwd`) are blocked for safety

## Installation Flow

1. **Fetch index** – Download the JSON from the repository URL
2. **Merge repos** – If multiple repos configured, merge indexes (highest version wins)
3. **Cache** – Save the merged index locally for offline queries
4. **Resolve** – For a package, recursively determine dependencies
5. **Download** – Fetch each package (including dependencies) from its URL
6. **Verify** – If SHA-256 provided, verify checksum before extraction
7. **Extract** – Unzip/untar into `$PREFIX`
8. **Record** – Save file manifest for removal later

## Security Considerations

- **SHA-256 verification** – Highly recommended to detect tampering
- **HTTPS only** – Repositories must use HTTPS; plain HTTP is rejected
- **Path validation** – Zip-slip and symlink-to-parent attacks are blocked
- **No checksums in repo** – Packages are not cryptographically signed (future work)

## Hosting Your Repository

### Static HTTP Server

```bash
# On your server, create an index.json and upload your .tar.gz files
# Make it accessible at, e.g., https://myrepo.example.com/index.json

curl https://myrepo.example.com/index.json
# Returns the JSON above
```

### GitHub Pages (Easy)

1. Fork or create a repo: `myrepo`
2. Upload packages to `packages/` folder
3. Generate `index.json` with build script
4. Push to `gh-pages` branch
5. Configure Nexus: `https://raw.githubusercontent.com/user/myrepo/gh-pages/index.json`

### Validation

Before publishing, validate your index:

```bash
# Using jq
jq '.' index.json > /dev/null && echo "Valid JSON"

# Check all URLs are reachable
jq -r '.packages[].url' index.json | while read url; do
  curl -I "$url" | head -1
done
```

## Versioning & Updates

When you release a new version:
1. Increment the version number in `index.json`
2. Upload the new package file
3. Update the SHA-256 hash
4. Commit and push

Nexus will automatically detect the new version and offer an update.

## Legacy / Custom Indexes

The app does not impose any format restrictions beyond the JSON schema. Custom fields are ignored. If you want to store additional metadata (build date, changelog, etc.), add them to the package object and Nexus will skip them gracefully.

---

**Questions?** Open an issue or contact the maintainers.
