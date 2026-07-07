import io
import json
import os
import urllib.error
from unittest.mock import MagicMock, patch

os.environ.setdefault("INTERNAL_API_BASE_URL", "http://internal-app.local")
os.environ.setdefault("INTERNAL_API_KEY", "test-key")

from handler import lambda_handler  # noqa: E402


def _fake_response(status, payload):
    response = MagicMock()
    response.status = status
    response.read.return_value = json.dumps(payload).encode("utf-8")
    response.__enter__.return_value = response
    return response


@patch("handler.urllib.request.urlopen")
def test_success_returns_upstream_body(mock_urlopen):
    mock_urlopen.return_value = _fake_response(200, {"success": True, "generated": 3})

    result = lambda_handler({}, None)

    assert result == {"statusCode": 200, "body": {"success": True, "generated": 3}}
    called_request = mock_urlopen.call_args[0][0]
    assert called_request.full_url == "http://internal-app.local/api/internal/reports/monthly-batch"
    assert called_request.get_header("X-internal-api-key") == "test-key"


@patch("handler.urllib.request.urlopen")
def test_http_error_is_captured_not_raised(mock_urlopen):
    mock_urlopen.side_effect = urllib.error.HTTPError(
        url="http://internal-app.local/api/internal/reports/monthly-batch",
        code=403,
        msg="Forbidden",
        hdrs=None,
        fp=io.BytesIO(b'{"success":false,"message":"invalid or missing internal api key"}'),
    )

    result = lambda_handler({}, None)

    assert result["statusCode"] == 403
    assert "invalid or missing internal api key" in result["error"]
