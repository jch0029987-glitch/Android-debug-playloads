class PayloadResult:
    def __init__(self, success: bool, output):
        self.success = success
        self.output = output

    def to_dict(self):
        return {"success": self.success, "output": self.output}
