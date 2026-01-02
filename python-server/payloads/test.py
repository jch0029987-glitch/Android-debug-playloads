from payloads.base import PayloadResult

def ping(args):
    return PayloadResult(True, "pong")

def echo(args):
    msg = args.get("msg", "")
    return PayloadResult(True, f"echo: {msg}")
