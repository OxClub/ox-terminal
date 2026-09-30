#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MAIN="$ROOT/app/src/main"
WORK="$(mktemp -d)"
mkdir -p "$MAIN/assets/linux" "$MAIN/jniLibs/arm64-v8a"

echo "=== 1/2 Debian rootfs (arm64) ==="
docker run --privileged --rm tonistiigi/binfmt --install arm64
cat > "$WORK/Dockerfile" << 'EOF'
FROM --platform=linux/arm64 debian:bookworm-slim
ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update && apt-get install -y --no-install-recommends \
      bash coreutils procps ca-certificates curl wget git openssh-client \
      python3 python3-pip nodejs build-essential make nano vim-tiny less \
      grep sed gawk findutils tar gzip xz-utils unzip zip file iputils-ping \
      openssl netcat-openbsd socat whois dnsutils \
      nmap hping3 nikto dirb gobuster hydra john hashcat sqlmap \
    && apt-get clean && rm -rf /var/lib/apt/lists/*
EOF
docker buildx build --platform linux/arm64 --load -t nexus-debian "$WORK"
CID="$(docker create --platform linux/arm64 nexus-debian)"
docker export "$CID" | gzip -6 > "$MAIN/assets/linux/rootfs.tar.gz"
docker rm "$CID" >/dev/null
ls -lh "$MAIN/assets/linux/rootfs.tar.gz"

echo "=== 2/2 PRoot (from the Termux package repository) ==="
REPO="https://packages.termux.dev/apt/termux-main"
curl -fsSL "$REPO/dists/stable/main/binary-aarch64/Packages" -o "$WORK/Packages"

deb_path() {
  awk -v p="$1" 'BEGIN{RS="";FS="\n"}
    { n="";f=""; for(i=1;i<=NF;i++){ if($i ~ /^Package: /) n=substr($i,10); if($i ~ /^Filename: /) f=substr($i,11) }
      if(n==p){print f; exit} }' "$WORK/Packages"
}

for pkg in proot libtalloc; do
  P="$(deb_path "$pkg")"
  [ -n "$P" ] || { echo "package $pkg not found in Termux repo"; exit 1; }
  echo "downloading $P"
  curl -fsSL "$REPO/$P" -o "$WORK/$pkg.deb"
  mkdir -p "$WORK/x-$pkg"
  dpkg-deb -x "$WORK/$pkg.deb" "$WORK/x-$pkg"
done

PROOT_BIN="$(find "$WORK/x-proot" -type f -name proot | head -1)"
LOADER="$(find "$WORK/x-proot" -type f -path '*libexec/proot/loader' | head -1)"
TALLOC="$(find -L "$WORK/x-libtalloc" -name 'libtalloc.so.2*' | head -1)"
[ -n "$PROOT_BIN" ] && [ -n "$LOADER" ] && [ -n "$TALLOC" ] || { echo "missing proot files"; find "$WORK" -maxdepth 6 | head -50; exit 1; }

cp -L "$PROOT_BIN" "$MAIN/jniLibs/arm64-v8a/libproot.so"
cp -L "$LOADER"    "$MAIN/jniLibs/arm64-v8a/libproot-loader.so"
cp -L "$TALLOC"    "$MAIN/jniLibs/arm64-v8a/libtalloc.so"
LOADER32="$(find "$WORK/x-proot" -type f -path '*libexec/proot/loader32' | head -1 || true)"
[ -n "$LOADER32" ] && cp -L "$LOADER32" "$MAIN/jniLibs/arm64-v8a/libproot-loader32.so" || true

echo "--- proot needs these libraries (for debugging) ---"
readelf -d "$MAIN/jniLibs/arm64-v8a/libproot.so" | grep -E 'NEEDED|RUNPATH|RPATH' || true
ls -lh "$MAIN/jniLibs/arm64-v8a/"
