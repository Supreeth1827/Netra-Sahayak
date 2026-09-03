import os

import cv2
import numpy as np
import torch

from PIL import Image
from torchvision import transforms

from pytorch_grad_cam import GradCAM
from pytorch_grad_cam.utils.image import show_cam_on_image
from pytorch_grad_cam.utils.model_targets import ClassifierOutputTarget

from services.prediction import model, DEVICE


IMG_SIZE = 300


transform = transforms.Compose([
    transforms.Resize((IMG_SIZE, IMG_SIZE)),
    transforms.ToTensor(),
    transforms.Normalize(
        mean=[0.485, 0.456, 0.406],
        std=[0.229, 0.224, 0.225]
    )
])


def generate_gradcam(
    image_path: str,
    output_path: str,
    target_class: int
):
    # Load original image
    image = Image.open(image_path).convert("RGB")

    # Resize image for visualization
    resized_image = image.resize((IMG_SIZE, IMG_SIZE))

    # Convert to RGB numpy image
    rgb_image = (
        np.array(resized_image).astype(np.float32) / 255.0
    )

    # Prepare model input
    input_tensor = (
        transform(image)
        .unsqueeze(0)
        .to(DEVICE)
    )

    # EfficientNet-B3 target layer
    target_layers = [model.conv_head]

    # Create Grad-CAM
    cam = GradCAM(
        model=model,
        target_layers=target_layers
    )

    # Explain the predicted class
    targets = [
        ClassifierOutputTarget(target_class)
    ]

    grayscale_cam = cam(
        input_tensor=input_tensor,
        targets=targets
    )[0]

    # Create heatmap overlay
    visualization = show_cam_on_image(
        rgb_image,
        grayscale_cam,
        use_rgb=True
    )

    # Make sure output folder exists
    output_directory = os.path.dirname(output_path)

    if output_directory:
        os.makedirs(
            output_directory,
            exist_ok=True
        )

    # Save heatmap
    success = cv2.imwrite(
        output_path,
        cv2.cvtColor(
            visualization,
            cv2.COLOR_RGB2BGR
        )
    )

    if not success:
        raise RuntimeError(
            f"Failed to save Grad-CAM heatmap: {output_path}"
        )

    if not os.path.exists(output_path):
        raise RuntimeError(
            f"Heatmap file was not created: {output_path}"
        )

    print(
        "Grad-CAM saved successfully:",
        output_path
    )

    return output_path
