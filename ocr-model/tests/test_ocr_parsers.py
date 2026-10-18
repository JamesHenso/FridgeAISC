import unittest
from types import SimpleNamespace

from ocr_parsers import product_label_fields, receipt_fields
from product_processor import classify_ocr_items


def field(content):
    return SimpleNamespace(content=content, confidence=0.95)


class OcrParserTests(unittest.TestCase):
    def test_bach_hoa_xanh_weight_and_price(self):
        item = {
            "valueObject": {
                "Description": field("Thịt ba chỉ heo (NK)"),
                "Quantity": field("0.356kg"),
                "Price": field("159,000"),
                "TotalPrice": field("56,604"),
            },
            "content": "Thịt ba chỉ heo (NK) 0.356kg 159,000 56,604",
        }
        receipt = SimpleNamespace(fields={
            "MerchantName": field("Bách hóa XANH"),
            "TransactionDate": field("26/04/2026 18:27"),
            "Subtotal": field("56,604"),
            "Total": field("398,591"),
            "Items": {"valueArray": [item]},
        })
        result = receipt_fields(SimpleNamespace(documents=[receipt]))
        row = classify_ocr_items(result["items"], result["purchased_at"])["items"][0]
        self.assertEqual((result["purchased_at"], result["total_vnd"]), ("2026-04-26", 398591))
        self.assertEqual(result["reconciliation_delta_vnd"], 0)
        self.assertEqual((row["qty_display"], row["unit_display"], row["price_vnd"]), (0.356, "kg", 56604))
        self.assertIsNone(row["expiry_date"])

    def test_package_size_is_not_purchase_quantity(self):
        item = {"valueObject": {"Description": field("Hạt nêm Heo 170g")},
                "content": "Hạt nêm Heo 170g 1 27,500 27,500"}
        receipt = SimpleNamespace(fields={"Items": {"valueArray": [item]}})
        self.assertIsNone(receipt_fields(SimpleNamespace(documents=[receipt]))["items"][0]["quantity"])

    def test_chicken_label_uses_hsd_not_unrelated_date(self):
        lines = ["MEATDeli Phi lê ức gà", "2062540005633050423", "Khối lượng tịnh:",
                 "563 g", "Đơn giá (đ/kg): 106,900", "Thành tiền (đ) 60,185",
                 "NSX: 05.04.23", "HSD: 09.04.23", "K.S.G.M: 26-01-01"]
        result = product_label_fields(SimpleNamespace(pages=[SimpleNamespace(
            lines=[SimpleNamespace(content=line) for line in lines])]))
        self.assertEqual((result["quantity"], result["unit"], result["price_vnd"]), (563, "g", 60185))
        self.assertEqual((result["manufactured_at"], result["expiry_date"]), ("2023-04-05", "2023-04-09"))

    def test_noisy_chicken_name_does_not_match_pear(self):
        result = classify_ocr_items([{"raw_name": "MEATDell Phí lê ức gà",
                                      "source_type": "product"}])["items"][0]
        self.assertIn("Ức gà", result["name"])
        self.assertEqual(result["match_status"], "needs_review")


if __name__ == "__main__":
    unittest.main()
