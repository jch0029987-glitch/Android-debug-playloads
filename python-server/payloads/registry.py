from payloads.base import PayloadResult
from payloads import test

PAYLOADS = {
    "ping": test.ping,
    "echo": test.echo,
}

def run_payload(name: str, args: dict):
    if name not in PAYLOADS:
        return PayloadResult(False, f"Unknown payload: {name}")
    try:
        return PAYLOADS[name](args)
    except Exception as e:
        return PayloadResult(False, str(e))
