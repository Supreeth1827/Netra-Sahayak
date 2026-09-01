"""Throwaway stand-in for the FastAPI backend, using only the stdlib.
Speaks the exact contract the Android app expects: multipart POST /predict
-> JSON, plus a static /results/<file> for the heatmap."""
import json, os, re, sys
from http.server import BaseHTTPRequestHandler, HTTPServer

HERE = os.path.dirname(os.path.abspath(__file__))

class H(BaseHTTPRequestHandler):
    def log_message(self, *a): sys.stderr.write("%s - %s\n" % (self.address_string(), a[0] % a[1:]))

    def do_POST(self):
        if not self.path.startswith("/predict"):
            self.send_error(404); return
        ctype = self.headers.get("Content-Type", "")
        length = int(self.headers.get("Content-Length", 0))
        body = self.rfile.read(length)
        # Report what we actually received so the test can assert on it.
        fields = re.findall(rb'name="([^"]+)"(?:; filename="([^"]+)")?', body)
        summary = [(n.decode(), (f or b"").decode()) for n, f in fields]
        print("RECEIVED ctype=%s bytes=%d parts=%s" % (ctype, length, summary), flush=True)
        payload = {
            "prediction": "Moderate Diabetic Retinopathy",
            "confidence": 0.91,
            "heatmap_url": "/results/abc123_heatmap.jpg",
            "recommendation": "Clinical evaluation by an ophthalmologist is recommended.",
        }
        data = json.dumps(payload).encode()
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def do_GET(self):
        if self.path.startswith("/results/"):
            path = os.path.join(HERE, "sample_heatmap.jpg")
            if os.path.exists(path):
                data = open(path, "rb").read()
                self.send_response(200)
                self.send_header("Content-Type", "image/jpeg")
                self.send_header("Content-Length", str(len(data)))
                self.end_headers()
                self.wfile.write(data)
                return
        self.send_error(404)

HTTPServer(("0.0.0.0", 8000), H).serve_forever()
