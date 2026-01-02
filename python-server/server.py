from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from payloads import registry
import traceback

app = FastAPI()


class PayloadRequest(BaseModel):
    payload: str = Field(..., description="Payload name to execute")
    args: dict = Field(default_factory=dict, description="Arguments for payload")


@app.get("/")
def root():
    return {"status": "ok"}


@app.get("/payloads")
def list_payloads():
    try:
        return {
            "success": True,
            "payloads": list(registry.PAYLOADS.keys()),
        }
    except Exception as e:
        return {
            "success": False,
            "error": str(e),
        }


@app.post("/payloads/run")
@app.post("/payloads/execute")
def execute(req: PayloadRequest):
    # Validate payload exists
    if not req.payload:
        raise HTTPException(status_code=400, detail="Missing payload name")

    if req.payload not in registry.PAYLOADS:
        raise HTTPException(
            status_code=404,
            detail=f"Unknown payload: {req.payload}",
        )

    try:
        result = registry.run_payload(req.payload, req.args)

        # Defensive check: payload must return expected object
        if not hasattr(result, "to_dict"):
            raise RuntimeError("Payload did not return a valid result object")

        return result.to_dict()

    except Exception as e:
        # Log full traceback on server (very important for you)
        traceback.print_exc()

        return {
            "success": False,
            "error": str(e),
            "type": e.__class__.__name__,
        }
