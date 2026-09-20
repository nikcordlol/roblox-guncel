"""
Roblox Güncel uygulaması için APK imzalama keystore'u üretir.
Tek seferlik çalıştırılır; üretilen release.keystore dosyası repoda tutulur
böylece her build aynı imzayla çıkar ve güncellemeler sorunsuz kurulur.
"""
from datetime import datetime, timezone
from pathlib import Path

from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.hazmat.primitives.serialization import pkcs12
from cryptography.x509.oid import NameOID

STORE_PASSWORD = b"robloxguncel2026"
ALIAS = "robloxguncel"
OUT = Path(__file__).resolve().parent.parent / "release.keystore"

key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
name = x509.Name(
    [
        x509.NameAttribute(NameOID.COMMON_NAME, "Roblox Guncel Updater"),
        x509.NameAttribute(NameOID.ORGANIZATION_NAME, "RobloxGuncel"),
        x509.NameAttribute(NameOID.COUNTRY_NAME, "TR"),
    ]
)
now = datetime.now(timezone.utc)
cert = (
    x509.CertificateBuilder()
    .subject_name(name)
    .issuer_name(name)
    .public_key(key.public_key())
    .serial_number(x509.random_serial_number())
    .not_valid_before(now)
    .not_valid_after(now.replace(year=now.year + 30))
    .sign(key, hashes.SHA256())
)

data = pkcs12.serialize_key_and_certificates(
    name=ALIAS.encode(),
    key=key,
    cert=cert,
    cas=None,
    encryption_algorithm=serialization.BestAvailableEncryption(STORE_PASSWORD),
)
OUT.write_bytes(data)
print(f"OK -> {OUT} ({len(data)} bytes)")
