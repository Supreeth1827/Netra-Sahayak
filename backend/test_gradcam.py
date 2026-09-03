from services.prediction import predict_image
from services.gradcam import generate_gradcam


IMAGE_PATH = "uploads/test.png"
OUTPUT_PATH = "heatmaps/test_heatmap.jpg"


# First get prediction
result = predict_image(IMAGE_PATH)

print("Prediction :", result["prediction"])
print("Confidence :", f'{result["confidence"]:.2%}')
print("Class Index:", result["class_index"])


# Generate Grad-CAM
heatmap_path = generate_gradcam(
    image_path=IMAGE_PATH,
    output_path=OUTPUT_PATH,
    target_class=result["class_index"]
)

print()
print("Grad-CAM generated successfully!")
print("Heatmap:", heatmap_path)