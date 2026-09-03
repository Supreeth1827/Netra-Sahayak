import os

import torch
import timm
from PIL import Image
from torchvision import transforms


# ---------------------------------------------------------
# Configuration
# ---------------------------------------------------------

MODEL_PATH = os.path.join(
    os.path.dirname(os.path.dirname(__file__)),
    "model",
    "efficientnet_b3_300_best.pth"
)

CLASS_NAMES = [
    "No DR",
    "Mild",
    "Moderate",
    "Severe",
    "Proliferative DR"
]

IMG_SIZE = 300

DEVICE = torch.device(
    "cuda" if torch.cuda.is_available() else "cpu"
)


# ---------------------------------------------------------
# Preprocessing
# Same preprocessing used by Member 2 / Member 3
# ---------------------------------------------------------

transform = transforms.Compose([
    transforms.Resize((IMG_SIZE, IMG_SIZE)),
    transforms.ToTensor(),
    transforms.Normalize(
        mean=[0.485, 0.456, 0.406],
        std=[0.229, 0.224, 0.225]
    )
])


# ---------------------------------------------------------
# Load model
# ---------------------------------------------------------

print("Loading EfficientNet-B3...")

model = timm.create_model(
    "efficientnet_b3",
    pretrained=False,
    num_classes=5
)

model.load_state_dict(
    torch.load(
        MODEL_PATH,
        map_location=DEVICE
    )
)

model = model.to(DEVICE)
model.eval()

print("Model loaded successfully!")
print("Device:", DEVICE)
print("Model:", MODEL_PATH)


# ---------------------------------------------------------
# Prediction function
# ---------------------------------------------------------

def predict_image(image_path: str):

    image = Image.open(image_path).convert("RGB")

    input_tensor = transform(image).unsqueeze(0).to(DEVICE)

    with torch.no_grad():
        outputs = model(input_tensor)

        probabilities = torch.softmax(
            outputs,
            dim=1
        )

    predicted_index = torch.argmax(
        probabilities,
        dim=1
    ).item()

    confidence = probabilities[
        0,
        predicted_index
    ].item()

    prediction = CLASS_NAMES[predicted_index]

    return {
        "prediction": prediction,
        "confidence": confidence,
        "class_index": predicted_index
    }