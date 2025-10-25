# 使用 Python 3.10 slim 版
FROM python:3.10-slim

# 設定工作目錄
WORKDIR /app

# 安裝系統依賴（libgomp1 是 OpenMP runtime，scipy / scikit-learn 會用到）
RUN apt-get update && apt-get install -y --no-install-recommends \
    libgomp1 \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

# 安裝 Python 套件
RUN pip install --no-cache-dir \
    prometheus-api-client \
    pandas \
    pycaret[full] \
    scikit-learn \
    matplotlib \
    requests

# 複製你的 Python 腳本
COPY aiops-demo.py .

# 設定容器啟動命令
CMD ["python", "-u", "aiops-demo.py"]
