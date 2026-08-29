import os
import sys
import urllib.request
import json
import uuid

def upload_file_io(file_path):
    print("Uploading to file.io via python standard library...")
    try:
        boundary = uuid.uuid4().hex
        filename = os.path.basename(file_path)
        
        with open(file_path, 'rb') as f:
            file_data = f.read()
            
        parts = []
        parts.append(f'--{boundary}'.encode('utf-8'))
        parts.append(f'Content-Disposition: form-data; name="file"; filename="{filename}"'.encode('utf-8'))
        parts.append(b'Content-Type: application/octet-stream')
        parts.append(b'')
        parts.append(file_data)
        parts.append(f'--{boundary}--'.encode('utf-8'))
        parts.append(b'')
        
        body = b'\r\n'.join(parts)
        
        req = urllib.request.Request(
            "https://file.io",
            data=body,
            headers={
                "Content-Type": f"multipart/form-data; boundary={boundary}",
                "Content-Length": str(len(body)),
                "User-Agent": "Mozilla/5.0"
            }
        )
        
        with urllib.request.urlopen(req, timeout=120) as response:
            res = json.loads(response.read().decode('utf-8'))
            if res.get("success"):
                link = res.get("link")
                print(f"SUCCESS_FILEIO: {link}")
                return link
            else:
                print(f"file.io failed: {res}")
    except Exception as e:
        print(f"file.io standard lib error: {e}")
    return None

if __name__ == "__main__":
    apk_path = "app/build/outputs/apk/debug/app-debug.apk"
    if not os.path.exists(apk_path):
        print(f"Error: APK not found at {apk_path}")
        sys.exit(1)
        
    print(f"Found APK. Size: {os.path.getsize(apk_path) / (1024*1024):.2f} MB")
    
    link = upload_file_io(apk_path)
    if link:
        print(f"\nFINAL_DOWNLOAD_LINK: {link}")
    else:
        print("\nAll uploads failed.")
