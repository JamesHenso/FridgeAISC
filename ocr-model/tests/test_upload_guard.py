import os
import unittest

os.environ.setdefault("AZURE_DOCUMENT_INTELLIGENCE_ENDPOINT", "https://example.invalid")
os.environ.setdefault("AZURE_DOCUMENT_INTELLIGENCE_KEY", "test-key")

from app import MAX_UPLOAD_BYTES, upload_problem


class UploadGuardTests(unittest.TestCase):
    def test_accepts_jpeg_png_pdf(self):
        self.assertIsNone(upload_problem(b"\xff\xd8\xff\xe0jpeg-bytes"))
        self.assertIsNone(upload_problem(b"\x89PNG\r\n\x1a\npng-bytes"))
        self.assertIsNone(upload_problem(b"%PDF-1.7"))

    def test_rejects_format_azure_refuses(self):
        # A picked WebP used to reach Azure declared as image/jpeg and came back as InvalidContent.
        self.assertIn("not a JPG", upload_problem(b"RIFF\x24\x00\x00\x00WEBPVP8 "))

    def test_rejects_oversized_image(self):
        self.assertIn("4 MB", upload_problem(b"\xff\xd8\xff" + b"0" * MAX_UPLOAD_BYTES))
