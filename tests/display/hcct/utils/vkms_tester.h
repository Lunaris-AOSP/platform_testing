/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#pragma once

#include "edid_helper.h"
#include <memory>
#include <string>
#include <unordered_map>
#include <vector>

namespace hcct {

// https://cs.android.com/android/platform/superproject/main/+/main:external/libdrm/include/drm/drm_mode.h;l=403
#define CONNECTOR_TYPES \
  CONNECTOR_TYPE(kUnknown, 0, "UNKNOWN") \
  CONNECTOR_TYPE(kVGA, 1, "VGA") \
  CONNECTOR_TYPE(kDisplayPort, 10, "DP") \
  CONNECTOR_TYPE(kHDMIA, 11, "HDMIA") \
  CONNECTOR_TYPE(keDP, 14, "eDP") \
  CONNECTOR_TYPE(kVirtual, 15, "VIRTUAL") \
  CONNECTOR_TYPE(kDSI, 16, "DSI") \
  CONNECTOR_TYPE(kDPI, 17, "DPI") \
  CONNECTOR_TYPE(kWriteback, 18, "WRITEBACK")

/**
 * @class VkmsTester
 * @brief Handles setup and configuration of Virtual KMS (VKMS) for display
 * emulation
 *
 * This class manages the creation of VKMS directory structures and file system
 * entries needed to configure virtual displays through the VKMS driver.
 */
class VkmsTester {
public:
  enum class ConnectorType {
    #define CONNECTOR_TYPE(enumName, value, stringName) enumName = value,
    CONNECTOR_TYPES
    #undef CONNECTOR_TYPE
  };

  class VkmsConnectorBuilder {
   public:
    // Create a builder with default settings
    static VkmsConnectorBuilder create() { return VkmsConnectorBuilder(); }

    VkmsConnectorBuilder& withType(ConnectorType type) {
      mType = type;
      return *this;
    }

    VkmsConnectorBuilder& withType(const std::string& typeStr) {
#define CONNECTOR_TYPE(enumName, value, stringName) \
  if (typeStr == stringName) mType = ConnectorType::enumName;
      CONNECTOR_TYPES
#undef CONNECTOR_TYPE
      return *this;
    }

    VkmsConnectorBuilder& enabledAtStart(bool enabled = true) {
      mEnabledAtStart = enabled;
      return *this;
    }

    VkmsConnectorBuilder& withAdditionalOverlayPlanes(int count) {
      mAdditionalOverlayPlanes = count;
      return *this;
    }

    VkmsConnectorBuilder& withMonitor(edid::MonitorName monitorName) {
      mMonitorName = monitorName;
      return *this;
    }

    // Friend access for VkmsTester to read the configuration
    friend class VkmsTester;

   private:
    ConnectorType mType = ConnectorType::kDisplayPort;
    bool mEnabledAtStart = true;
    int mAdditionalOverlayPlanes = 0;
    edid::MonitorName mMonitorName;
  };

  /**
   * Creates a VKMS configuration with a specified number of virtual displays,
   * each with a default setup.
   *
   * Each connector is configured with:
   *   - 1 CRTC
   *   - 1 Encoder
   *   - 2 Planes: 1 Primary and 1 Cursor
   *
   * The first connector is set to eDP, and the remaining connectors are set to
   * DisplayPort.
   *
   * @param displaysCount The number of virtual displays to configure.
   * @return A unique pointer to the created VkmsTester instance, or nullptr if
   * creation failed.
   */
  static std::unique_ptr<VkmsTester>
  CreateWithGenericConnectors(int displaysCount);

  /**
   * Creates a VKMS configuration based on a provided vector of connector
   * builders.
   *
   * This method allows for fine-grained control over the configuration of each
   * virtual display. Each builder in the vector defines a single connector and
   * its associated configuration.
   *
   * @param builders A vector of VkmsConnectorBuilder objects, each defining the
   * configuration for a single connector. The size of the vector determines the
   * number of virtual displays to create.
   * @return A unique pointer to the created VkmsTester instance, or nullptr if
   * creation failed.
   */
  static std::unique_ptr<VkmsTester> CreateWithBuilders(
      const std::vector<VkmsConnectorBuilder>& builders);

  static void ForceDeleteVkmsDir();

  ~VkmsTester();

  // Returns the number of connectors that have been successfully created
  // regardless of their connection status.
  int getActiveConnectorsCount() const { return mActiveConnectorsCount; }
  bool ToggleConnector(int connectorIndex, bool enable);

  // Prevent the VkmsTester instance from cleaning up the VKMS directories upon
  // destruction.
  void DisableCleanupOnDestruction();

private:
  enum class DrmResource {
    kConnector,
    kCrtc,
    kEncoder,
    kPlane,
  };

  // https://cs.android.com/android/platform/superproject/main/+/main:external/libdrm/xf86drmMode.h;l=225
  enum class PlaneType {
    kOverlay = 0,
    kPrimary = 1,
    kCursor = 2,
  };

  // Create a map of the base directory for each resource type to maintain
  // string consistency throughout the code.
  const std::unordered_map<DrmResource, std::string> kDrmResourceBase = {
      {DrmResource::kConnector, "connectors/CONNECTOR_"},
      {DrmResource::kCrtc, "crtcs/CRTC_"},
      {DrmResource::kEncoder, "encoders/ENCODER_"},
      {DrmResource::kPlane, "planes/PLANE_"},
  };

  // Private constructor to prevent direct instantiation without the Create
  // functions.
  explicit VkmsTester(size_t displaysCount,
                      const std::vector<VkmsConnectorBuilder>& builders = {});

  bool SetVkmsAsDisplayDriver();
  bool SetupDisplays(int displaysCount,
                     const std::vector<VkmsConnectorBuilder>& builders);
  static bool ToggleVkms(bool enable);
  static bool ToggleHwc3(bool enable);

  bool CreateResource(DrmResource resource, int index);
  bool SetConnectorStatus(int index, bool enable);
  bool SetConnectorType(int index, ConnectorType type);
  bool SetConnectorEdid(int index, edid::MonitorName monitorName);
  bool SetPlaneType(int index, PlaneType type);
  bool SetPlaneFormat(int index);
  bool LinkToCrtc(DrmResource resource, int resourceIdx, int crtcIdx);
  bool LinkConnectorToEncoder(int connectorIdx, int encoderIdx);

  static void ShutdownAndCleanUpVkms();
  static void FindAndCleanupPossibleLinks(const std::string &dirPath);
  static void CleanUpDirAndChildren(const std::string &rootDir);

  size_t mActiveConnectorsCount = 0;
  // Used to track the most recently created plane ID, as the number of planes
  // can vary per connector. This value is updated whenever a new plane is
  // created.
  int mLatestPlaneId = 0;

  bool mDisableCleanupOnDestruction = false;
  bool mInitialized = false;
};

} // namespace hcct