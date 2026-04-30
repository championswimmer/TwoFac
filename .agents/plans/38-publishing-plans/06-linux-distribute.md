# Linux Distribution Guide — TwoFac v1.6.0

> **App**: TwoFac (2FA Authenticator)
> **Package name**: `twofac`
> **App ID**: `tech.arnav.TwoFac`
> **Built with**: Compose Desktop (JVM 21, Kotlin Multiplatform)
> **Build command**: `./gradlew packageDeb` → produces a `.deb` with bundled JVM
> **Icon**: `composeApp/src/desktopMain/resources/icons/linux.png`
> **Category**: Security / Utilities
> **Note**: Uses `dorkbox/SystemTray` for native Linux tray integration

---

## 1. Snap Store

**Docs**: [snapcraft.io/docs](https://snapcraft.io/docs)

### Account & Name Registration
1. Register at [snapcraft.io/account](https://snapcraft.io/account) (free)
2. `sudo snap install snapcraft --classic`
3. `snapcraft login`
4. `snapcraft register twofac` — reserves the snap name

### `snapcraft.yaml`

Create `snap/snapcraft.yaml` in the project root:

```yaml
name: twofac
version: '1.6.0'
summary: Two-factor authentication app
description: |
  TwoFac is a cross-platform 2FA authenticator built with
  Compose Desktop. Generates TOTP/HOTP codes for your accounts.

grade: stable
confinement: strict          # see note below
base: core24

apps:
  twofac:
    command: twofac/bin/TwoFac    # path inside the bundled DEB tree
    extensions: [gnome]
    plugs:
      - home                     # config storage (~/.config/twofac)
      - network                  # time sync, cloud backup
      - desktop
      - desktop-legacy
      - wayland
      - x11
      - opengl

parts:
  twofac:
    plugin: dump
    source: composeApp/build/compose/binaries/main/deb/
    source-type: local
    override-build: |
      cd $SNAPCRAFT_PROJECT_DIR
      ./gradlew packageDeb --no-daemon
      # Extract the deb contents into the snap
      dpkg-deb -x composeApp/build/compose/binaries/main/deb/twofac_1.6.0-1_amd64.deb $SNAPCRAFT_PART_INSTALL/
    stage-packages:
      - libayatana-appindicator3-1   # SystemTray support
```

### Confinement
| Mode | Use case |
|------|----------|
| `strict` | **Recommended.** Full sandbox; `home` plug gives `~/.config` access for config storage |
| `classic` | No sandbox. Only needed if raw filesystem access is required. Requires Snap Store review |
| `devmode` | Development/testing only |

### Build, Test, Publish
```bash
snapcraft                                          # builds .snap
sudo snap install --dangerous twofac_1.6.0_amd64.snap   # local test
snapcraft upload twofac_1.6.0_amd64.snap           # upload
snapcraft release twofac <revision> stable          # release to channel
```

### Channels
- **edge** → automatic on upload (CI/testing)
- **beta** / **candidate** → manual promotion
- **stable** → production; may require review for `classic` confinement
- Users get auto-updates by default

**Key docs**:
- [Confinement](https://snapcraft.io/docs/snap-confinement)
- [Publishing](https://snapcraft.io/docs/releasing-your-app)
- [Desktop extensions](https://snapcraft.io/docs/gnome-extension)

---

## 2. Flatpak (Flathub)

**Docs**: [docs.flatpak.org](https://docs.flatpak.org/en/latest/) · [docs.flathub.org](https://docs.flathub.org/docs/)

### App ID
`tech.arnav.TwoFac` (reverse-DNS matching your domain)

### Runtime & SDK
- **Runtime**: `org.freedesktop.Platform//24` (or latest stable)
- **SDK**: `org.freedesktop.Sdk//24`
- **JVM extension**: `org.freedesktop.Sdk.Extension.openjdk21`

### Flatpak Manifest

Create `tech.arnav.TwoFac.json`:

```json
{
  "app-id": "tech.arnav.TwoFac",
  "runtime": "org.freedesktop.Platform",
  "runtime-version": "24",
  "sdk": "org.freedesktop.Sdk",
  "sdk-extensions": ["org.freedesktop.Sdk.Extension.openjdk21"],
  "command": "twofac",
  "finish-args": [
    "--share=ipc",
    "--socket=x11",
    "--socket=wayland",
    "--device=dri",
    "--share=network",
    "--filesystem=xdg-config/twofac:create",
    "--talk-name=org.kde.StatusNotifierWatcher",
    "--talk-name=org.freedesktop.Notifications"
  ],
  "modules": [
    {
      "name": "twofac",
      "buildsystem": "simple",
      "build-commands": [
        "/usr/lib/sdk/openjdk21/install.sh",
        "install -Dm755 twofac.sh /app/bin/twofac",
        "install -Dm644 twofac.jar /app/lib/twofac.jar",
        "install -Dm644 tech.arnav.TwoFac.desktop /app/share/applications/tech.arnav.TwoFac.desktop",
        "install -Dm644 tech.arnav.TwoFac.metainfo.xml /app/share/metainfo/tech.arnav.TwoFac.metainfo.xml",
        "install -Dm644 linux.png /app/share/icons/hicolor/256x256/apps/tech.arnav.TwoFac.png"
      ],
      "sources": [
        {
          "type": "archive",
          "url": "https://github.com/nicecoder/twofac/releases/download/v1.6.0/twofac-linux-1.6.0.tar.gz",
          "sha256": "<hash>"
        }
      ]
    }
  ]
}
```

### Required Metadata Files

**Desktop file** (`tech.arnav.TwoFac.desktop`):
```ini
[Desktop Entry]
Name=TwoFac
Comment=Two-factor authentication app
Exec=twofac
Icon=tech.arnav.TwoFac
Terminal=false
Type=Application
Categories=Security;Utility;
StartupWMClass=TwoFac
```

**AppStream metadata** (`tech.arnav.TwoFac.metainfo.xml`):
```xml
<?xml version="1.0" encoding="UTF-8"?>
<component type="desktop-application">
  <id>tech.arnav.TwoFac</id>
  <name>TwoFac</name>
  <summary>Two-factor authentication manager</summary>
  <metadata_license>CC0-1.0</metadata_license>
  <project_license>MIT</project_license>
  <developer id="tech.arnav">
    <name>Arnav Gupta</name>
    <url>https://arnav.tech</url>
  </developer>
  <url type="homepage">https://twofac.app</url>
  <releases>
    <release version="1.6.0" date="2025-01-01"/>
  </releases>
  <content_rating type="oars-1.1"/>
</component>
```

### Build & Test
```bash
flatpak-builder --force-clean build-dir tech.arnav.TwoFac.json
flatpak-builder --run build-dir tech.arnav.TwoFac.json twofac
```

### Submit to Flathub
1. Fork [github.com/flathub/flathub](https://github.com/flathub/flathub)
2. Create branch `new-pr` with your manifest, desktop file, AppStream XML, and icons
3. Open a PR — reviewers check sandboxing, metadata, and build
4. Approval typically takes 1–7 days
5. After merge, Flathub CI auto-builds on new tagged releases

**Key docs**:
- [Flatpak manifest reference](https://docs.flatpak.org/en/latest/manifests.html)
- [Flathub submission guidelines](https://docs.flathub.org/docs/for-app-authors/submission/)
- [AppStream metadata spec](https://www.freedesktop.org/software/appstream/docs/)

---

## 3. PPA (Debian/Ubuntu)

**Docs**: [help.launchpad.net/Packaging/PPA](https://help.launchpad.net/Packaging/PPA)

### Account Setup
1. Register at [launchpad.net](https://launchpad.net)
2. Create a PPA: Your profile → "Create a new PPA"
   - Name: `twofac` → users add via `ppa:youruser/twofac`

### GPG Key Setup
```bash
gpg --full-generate-key                 # RSA 4096, no expiry
gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>
# Add key fingerprint to Launchpad: profile → OpenPGP keys
```

### Prepare Source Package

Launchpad builds from **source packages**, not binary DEBs. Wrap the Compose Desktop DEB build:

```
twofac-1.6.0/
├── debian/
│   ├── control          # Package metadata, depends, description
│   ├── changelog        # Version + target suite (e.g., noble, jammy)
│   ├── rules            # Build rules (calls ./gradlew packageDeb)
│   ├── copyright        # License info
│   └── compat           # Debhelper compat level
├── gradlew
├── build.gradle.kts
└── ...
```

Build the source package:
```bash
debuild -S -sa -k<KEY_ID>              # creates .dsc, .changes, .tar.gz
```

### Upload with dput
```bash
# ~/.dput.cf
# [twofac-ppa]
# fqdn = ppa.launchpadcontent.net
# method = https
# incoming = ~youruser/+archive/ubuntu/twofac/
# allow_unsigned_uploads = 0

dput twofac-ppa twofac_1.6.0-1_source.changes
```

### Multiple Ubuntu Versions
- Edit `debian/changelog` to target each suite (`noble`, `jammy`, `focal`)
- Upload a separate source package per suite (`~noble1`, `~jammy1` version suffix)
- Launchpad builds for each automatically

### Alternative: Self-Hosted APT Repo (GitHub Pages + reprepro)

For publishing the binary DEB from `./gradlew packageDeb` directly:

```bash
sudo apt install reprepro
mkdir -p apt-repo/conf

# apt-repo/conf/distributions
cat <<EOF > apt-repo/conf/distributions
Origin: TwoFac
Label: TwoFac
Codename: stable
Architectures: amd64
Components: main
Description: TwoFac APT Repository
SignWith: <GPG_KEY_ID>
EOF

# Add the DEB
reprepro -b apt-repo includedeb stable \
  composeApp/build/compose/binaries/main/deb/twofac_1.6.0-1_amd64.deb

# Push apt-repo/ to GitHub Pages branch
```

Users add:
```bash
curl -fsSL https://youruser.github.io/twofac-apt/KEY.gpg | sudo gpg --dearmor -o /usr/share/keyrings/twofac.gpg
echo "deb [signed-by=/usr/share/keyrings/twofac.gpg] https://youruser.github.io/twofac-apt/ stable main" \
  | sudo tee /etc/apt/sources.list.d/twofac.list
sudo apt update && sudo apt install twofac
```

**Key docs**:
- [Ubuntu Packaging Guide](https://ubuntu-packaging-guide.readthedocs.io/en/latest/)
- [Launchpad PPA docs](https://help.launchpad.net/Packaging/PPA)
- [reprepro manpage](https://manpages.debian.org/stable/reprepro/reprepro.1.en.html)

---

## 4. RPM Repository (Fedora / RHEL)

**Docs**: [copr.fedorainfracloud.org](https://copr.fedorainfracloud.org/) · [docs.fedoraproject.org/en-US/packaging-guidelines](https://docs.fedoraproject.org/en-US/packaging-guidelines/)

### Option A: Convert DEB → RPM with `alien`

Quick but imperfect — useful for testing:
```bash
sudo apt install alien
sudo alien -r twofac_1.6.0-1_amd64.deb    # → twofac-1.6.0-2.x86_64.rpm
```

For more control, generate a spec file:
```bash
sudo alien -r -g -v twofac_1.6.0-1_amd64.deb
# Edit the generated .spec, then:
rpmbuild --buildroot $(pwd) -bb twofac-1.6.0.spec
```

### Option B: Create a Proper RPM Spec File

`twofac.spec`:
```spec
Name:           twofac
Version:        1.6.0
Release:        1%{?dist}
Summary:        Two-factor authentication app
License:        MIT
URL:            https://twofac.app
Source0:        twofac-1.6.0-linux.tar.gz

AutoReqProv:    no

%description
TwoFac is a cross-platform 2FA authenticator with TOTP/HOTP support.
Built with Compose Desktop (JVM bundled).

%install
mkdir -p %{buildroot}/opt/twofac
tar xf %{SOURCE0} -C %{buildroot}/opt/twofac
mkdir -p %{buildroot}/usr/bin
ln -s /opt/twofac/bin/TwoFac %{buildroot}/usr/bin/twofac
mkdir -p %{buildroot}/usr/share/applications
install -Dm644 twofac.desktop %{buildroot}/usr/share/applications/twofac.desktop
mkdir -p %{buildroot}/usr/share/icons/hicolor/256x256/apps
install -Dm644 linux.png %{buildroot}/usr/share/icons/hicolor/256x256/apps/twofac.png

%files
/opt/twofac/
/usr/bin/twofac
/usr/share/applications/twofac.desktop
/usr/share/icons/hicolor/256x256/apps/twofac.png
```

Build: `rpmbuild -bb twofac.spec`

### Publish to COPR
1. Log in at [copr.fedorainfracloud.org](https://copr.fedorainfracloud.org/) with your FAS account
2. Create a new project: `twofac`
3. Upload `.src.rpm` or point COPR to your spec file + source tarball via SCM
4. Select target chroots (e.g., `fedora-41-x86_64`, `fedora-rawhide-x86_64`)
5. COPR builds and hosts the RPM repo automatically

Users enable:
```bash
sudo dnf copr enable youruser/twofac
sudo dnf install twofac
```

### GPG Signing for RPMs
```bash
# Generate key (if not already)
gpg --full-generate-key

# Configure rpm macros
echo "%_gpg_name Your Name <you@example.com>" >> ~/.rpmmacros

# Sign
rpm --addsign twofac-1.6.0-1.x86_64.rpm
```

### Alternative: Self-Hosted YUM/DNF Repo
```bash
sudo dnf install createrepo_c

mkdir -p rpm-repo/
cp twofac-1.6.0-1.x86_64.rpm rpm-repo/
createrepo_c rpm-repo/

# Sign the repodata
gpg --detach-sign --armor rpm-repo/repodata/repomd.xml

# Host on any static server (GitHub Pages, S3, etc.)
```

Users add `/etc/yum.repos.d/twofac.repo`:
```ini
[twofac]
name=TwoFac Repository
baseurl=https://youruser.github.io/twofac-rpm/
gpgcheck=1
gpgkey=https://youruser.github.io/twofac-rpm/KEY.gpg
enabled=1
```

**Key docs**:
- [COPR user documentation](https://docs.pagure.org/copr.copr/user_documentation.html)
- [RPM Packaging Guide](https://rpm-packaging-guide.github.io/)
- [Fedora Packaging Guidelines](https://docs.fedoraproject.org/en-US/packaging-guidelines/)

---

## Pre-Launch Checklist

### General
- [ ] Linux icon exists at `composeApp/src/desktopMain/resources/icons/linux.png` (256×256+ PNG)
- [ ] `./gradlew packageDeb` builds successfully
- [ ] `.desktop` file created with correct `StartupWMClass`
- [ ] AppStream `metainfo.xml` passes validation (`appstreamcli validate`)
- [ ] GPG key generated and published to keyservers

### Snap Store
- [ ] Account created at snapcraft.io
- [ ] `twofac` snap name registered
- [ ] `snap/snapcraft.yaml` created and builds locally
- [ ] Tested locally with `--dangerous` install
- [ ] Uploaded to edge channel and tested
- [ ] Promoted to stable

### Flatpak (Flathub)
- [ ] `tech.arnav.TwoFac.json` manifest created
- [ ] Desktop file and AppStream metadata pass validation
- [ ] Local build with `flatpak-builder` succeeds
- [ ] PR opened to `flathub/flathub`
- [ ] PR approved and merged
- [ ] Verified on Flathub listing

### PPA (Debian/Ubuntu)
- [ ] Launchpad account created
- [ ] PPA `ppa:youruser/twofac` created
- [ ] GPG key linked to Launchpad account
- [ ] Source package built with `debuild -S`
- [ ] Uploaded via `dput` and builds succeeded
- [ ] Tested `add-apt-repository` + `apt install` on target Ubuntu versions
- [ ] _(Alt)_ Self-hosted APT repo set up with reprepro + GitHub Pages

### RPM (Fedora/COPR)
- [ ] RPM spec file created (or DEB converted with `alien`)
- [ ] COPR project created at copr.fedorainfracloud.org
- [ ] RPM builds for target Fedora versions
- [ ] Tested `dnf copr enable` + `dnf install`
- [ ] RPM is GPG-signed
- [ ] _(Alt)_ Self-hosted YUM repo set up with createrepo_c
