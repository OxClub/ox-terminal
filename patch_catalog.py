import json, pathlib

path = pathlib.Path("app/src/main/assets/distros/catalog.json")
data = json.loads(path.read_text())

# Desktop-only distros: no minimal rootfs tarball exists anywhere for these.
remove_ids = {"pentoo", "nixos", "antiX", "mx", "elementary", "popos", "solus", "mandriva", "artix", "linuxmint", "zorin"}

# Server distros: swap Dockerfile links for real Jenkins-built rootfs tarballs.
fix_urls = {
    "opensuse": "https://jenkins.linuxcontainers.org/job/image-opensuse/architecture=arm64,release=15.6,variant=default/lastStableBuild/artifact/rootfs.tar.xz",
    "opensuseTumbleweed": "https://jenkins.linuxcontainers.org/job/image-opensuse/architecture=arm64,release=tumbleweed,variant=default/lastStableBuild/artifact/rootfs.tar.xz",
    "centos9": "https://jenkins.linuxcontainers.org/job/image-centos/architecture=arm64,release=9-Stream,variant=default/lastStableBuild/artifact/rootfs.tar.xz",
    "devuan": "https://jenkins.linuxcontainers.org/job/image-devuan/architecture=arm64,release=daedalus,variant=default/lastStableBuild/artifact/rootfs.tar.xz",
}

before = len(data["distros"])
data["distros"] = [d for d in data["distros"] if d["id"] not in remove_ids]
removed = before - len(data["distros"])

fixed = 0
for d in data["distros"]:
    if d["id"] in fix_urls:
        d["url"] = fix_urls[d["id"]]
        fixed += 1

path.write_text(json.dumps(data, indent=2))
print(f"Removed {removed} dead desktop-distro entries")
print(f"Fixed {fixed} server-distro URLs")
print("Remaining distro count:", len(data["distros"]))
