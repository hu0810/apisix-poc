from prometheus_api_client import PrometheusConnect, MetricRangeDataFrame
from pycaret.anomaly import setup, create_model, assign_model, save_model, load_model
import pandas as pd
import datetime
import time
import os

print("🚀 AIops demo container started")

# Prometheus client
prom = PrometheusConnect(url="http://prometheus:9090", disable_ssl=True)

# 模型儲存路徑
MODEL_DIR = "/app/model"
MODEL_NAME = "cpu_iforest_model"
MODEL_PATH = os.path.join(MODEL_DIR, MODEL_NAME)

# 確保目錄存在
os.makedirs(MODEL_DIR, exist_ok=True)

# retrain model function
def retrain_model(days=7):
    try:
        print(f"🔄 Retraining model with last {days} days of data...")
        end_time = datetime.datetime.now()
        start_time = end_time - datetime.timedelta(days=days)

        metric_data = prom.get_metric_range_data(
            metric_name='process_cpu_usage',
            start_time=start_time,
            end_time=end_time
        )

        df = MetricRangeDataFrame(metric_data)
        if df.empty or 'value' not in df.columns:
            print("⚠️ No historical data available for retraining.")
            return None

        df.reset_index(inplace=True)
        df = df.rename(columns={'value': 'cpu_usage'})
        df['cpu_usage'] = pd.to_numeric(df['cpu_usage'], errors='coerce')
        df = df.dropna(subset=['cpu_usage'])

        if df.empty:
            print("⚠️ All cpu_usage values are NaN.")
            return None

        s = setup(data=df[['cpu_usage']], session_id=123, verbose=False)
        model = create_model('iforest')
        save_model(model, MODEL_PATH)
        print(f"✅ Model retrained and saved at {MODEL_PATH}.pkl")
        return model
    except Exception as e:
        print(f"❌ Error during retrain: {e}")
        return None

# detect anomalies function
def detect_anomalies():
    try:
        if not os.path.exists(MODEL_PATH + ".pkl"):
            print("⚠️ Model not found, retraining first...")
            model = retrain_model()
            if model is None:
                return
        else:
            model = load_model(MODEL_PATH)

        end_time = datetime.datetime.now()
        start_time = end_time - datetime.timedelta(minutes=5)
        metric_data = prom.get_metric_range_data(
            metric_name='process_cpu_usage',
            start_time=start_time,
            end_time=end_time
        )
        df = MetricRangeDataFrame(metric_data)
        if df.empty or 'value' not in df.columns:
            print("⚠️ No recent data to detect.")
            return

        df.reset_index(inplace=True)
        df = df.rename(columns={'value': 'cpu_usage'})
        df['cpu_usage'] = pd.to_numeric(df['cpu_usage'], errors='coerce')
        df = df.dropna(subset=['cpu_usage'])
        if df.empty:
            print("⚠️ All recent cpu_usage values are NaN.")
            return

        results = predict_model(model, data=df[['cpu_usage']])
        anomalies = results[results['Anomaly'] == 1]
        print(f"🔍 Detected {len(anomalies)} anomalies in last 5 minutes")
        if not anomalies.empty:
            print(anomalies.tail(5)[['cpu_usage', 'Anomaly_Score']])
        print("="*60)

    except Exception as e:
        print(f"❌ Error during detect_anomalies: {e}")

# main loop
RETRAIN_HOUR = 0  # 每天凌晨 0 點 retrain，可調整
while True:
    now = datetime.datetime.now()
    if now.hour == RETRAIN_HOUR and (not os.path.exists(MODEL_PATH + ".pkl") or (now.date() != datetime.datetime.fromtimestamp(os.path.getmtime(MODEL_PATH + ".pkl")).date())):
        retrain_model(days=7)

    detect_anomalies()
    time.sleep(60)
