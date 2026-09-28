from fastapi import FastAPI
from app.routes.answer_routes import router as answer_router
from app.routes.batch_routes import router as batch_router

app = FastAPI(title="Employee Policy and Reimbursement Triage", version="2.0.0")


@app.get("/health")
def health_check():
    return {"status": "UP"}


app.include_router(answer_router)
app.include_router(batch_router)
