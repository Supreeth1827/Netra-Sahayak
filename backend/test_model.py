from services.prediction import predict_image


IMAGE_PATH = "uploads/test.png"


result = predict_image(IMAGE_PATH)

print("\n==============================")
print("MODEL PREDICTION TEST")
print("==============================")
print("Prediction :", result["prediction"])
print("Confidence :", f'{result["confidence"]:.2%}')
print("Class Index:", result["class_index"])
print("==============================")