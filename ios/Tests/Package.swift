// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "CapacitorFileSharerTests",
    platforms: [.iOS(.v15)],
    dependencies: [
        .package(path: "../..")
    ],
    targets: [
        .testTarget(
            name: "CapacitorFileSharerTests",
            dependencies: [
                .product(name: "CapacitorFileSharer", package: "capacitor-filesharer")
            ],
            path: "CapacitorFileSharerTests"
        )
    ]
)
