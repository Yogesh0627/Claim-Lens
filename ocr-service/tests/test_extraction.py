"""Field-extraction unit tests. No Tesseract needed — these exercise the regex heuristics only."""

from app.extraction.fields import (
    extract_amounts,
    extract_fields,
    extract_registration_numbers,
)


def test_registration_number_variants_normalise_to_one_form():
    assert extract_registration_numbers("Vehicle MH 12 AB 1234 shown") == ["MH12AB1234"]
    assert extract_registration_numbers("reg mh-12-ab-1234") == ["MH12AB1234"]
    assert extract_registration_numbers("KA01MJ9999 and KA 01 MJ 9999") == ["KA01MJ9999"]


def test_bh_series_registration():
    assert "22BH1234AB" in extract_registration_numbers("New plate 22 BH 1234 AB")


def test_amounts_parse_currency_and_commas():
    amounts = extract_amounts("Estimate Rs. 1,25,000 and total ₹ 47500.50 INR 999")
    assert 125000.0 in amounts
    assert 47500.5 in amounts
    assert 999.0 in amounts


def test_extract_fields_pulls_policy_dates_chassis():
    text = (
        "Policy No: MOT-2024-0098\n"
        "Chassis No MA3ERLF1S00123456\n"
        "Engine Number: K12MN1234567\n"
        "Date of loss 01/06/2024, issued 2024-04-01\n"
        "Registration MH12AB1234\n"
        "Repair estimate Rs. 50,000"
    )
    fields = extract_fields(text)
    assert fields.policy_numbers == ["MOT-2024-0098"]
    assert "MA3ERLF1S00123456" in fields.chassis_numbers
    assert "K12MN1234567" in fields.engine_numbers
    assert "MH12AB1234" in fields.registration_numbers
    assert "01/06/2024" in fields.dates and "2024-04-01" in fields.dates
    assert 50000.0 in fields.amounts


def test_empty_text_yields_empty_fields():
    fields = extract_fields("")
    assert fields.registration_numbers == []
    assert fields.amounts == []
