# TypeScript Implementation Guide

Complete guide for implementing the TypeScript layer of Capacitor plugins.

## Project Structure

```
src/
├── definitions.ts    # Interface definitions (contract)
├── web.ts            # Web platform implementation
└── index.ts          # Plugin registration and exports
```

---

## 1. Definitions (`src/definitions.ts`)

### Basic Plugin Interface

```typescript
import type { PluginListenerHandle } from '@capacitor/core';

/**
 * MyPlugin provides [brief description]
 */
export interface MyPluginPlugin {
  /**
   * Method description
   * @param options - Input parameters
   * @returns Promise resolving to result
   * @throws {Error} ERROR_CODE - When error occurs
   * @since 1.0.0
   */
  methodName(options: MethodOptions): Promise<MethodResult>;
}

/**
 * Options for methodName()
 */
export interface MethodOptions {
  /**
   * Parameter description
   */
  param1: string;

  /**
   * Optional parameter with default
   * @default 0
   */
  param2?: number;
}

/**
 * Result from methodName()
 */
export interface MethodResult {
  success: boolean;
  data: string;
}
```

### Plugin with Events

```typescript
export interface SensorPluginPlugin {
  /**
   * Start sensor monitoring
   */
  startMonitoring(options: SensorOptions): Promise<void>;

  /**
   * Stop sensor monitoring
   */
  stopMonitoring(): Promise<void>;

  /**
   * Listen for sensor data updates
   */
  addListener(
    eventName: 'sensorData',
    listenerFunc: (event: SensorData) => void,
  ): Promise<PluginListenerHandle>;

  /**
   * Listen for sensor errors
   */
  addListener(
    eventName: 'sensorError',
    listenerFunc: (error: SensorError) => void,
  ): Promise<PluginListenerHandle>;

  /**
   * Remove all listeners
   */
  removeAllListeners(): Promise<void>;
}

export interface SensorOptions {
  /**
   * Update interval in milliseconds
   * @default 1000
   */
  interval?: number;

  /**
   * Sensor type to monitor
   */
  sensorType: 'accelerometer' | 'gyroscope' | 'magnetometer';
}

export interface SensorData {
  x: number;
  y: number;
  z: number;
  timestamp: number;
}

export interface SensorError {
  code: string;
  message: string;
}
```

### Plugin with Permissions

```typescript
export interface CameraPluginPlugin {
  /**
   * Check current permission status
   */
  checkPermissions(): Promise<PermissionStatus>;

  /**
   * Request permissions from user
   */
  requestPermissions(): Promise<PermissionStatus>;

  /**
   * Capture photo
   * @throws {Error} PERMISSION_DENIED if permission not granted
   */
  getPhoto(options: PhotoOptions): Promise<Photo>;
}

export interface PermissionStatus {
  camera: PermissionState;
  photos: PermissionState;  // iOS photo library
}

export type PermissionState = 'granted' | 'denied' | 'prompt';

export interface PhotoOptions {
  quality?: number;
  source?: 'camera' | 'gallery';
}

export interface Photo {
  base64String?: string;
  path?: string;
  format: 'jpeg' | 'png';
}
```

---

## 2. Web Implementation (`src/web.ts`)

### Basic Web Plugin

```typescript
import { WebPlugin } from '@capacitor/core';
import type { MyPluginPlugin, MethodOptions, MethodResult } from './definitions';

export class MyPluginWeb extends WebPlugin implements MyPluginPlugin {
  /**
   * Implement plugin method for web platform
   */
  async methodName(options: MethodOptions): Promise<MethodResult> {
    console.log('MyPlugin.methodName called with:', options);

    // Validate parameters
    if (!options.param1) {
      throw this.createError(
        'INVALID_PARAMETER',
        'param1 is required'
      );
    }

    // Implement web-specific logic
    try {
      const result = await this.performWebOperation(options);
      return {
        success: true,
        data: result,
      };
    } catch (error) {
      throw this.createError(
        'OPERATION_FAILED',
        `Operation failed: ${error.message}`
      );
    }
  }

  /**
   * Helper method for web-specific logic
   */
  private async performWebOperation(options: MethodOptions): Promise<string> {
    // Web implementation here
    return `Processed: ${options.param1}`;
  }

  /**
   * Create standardized error
   */
  private createError(code: string, message: string): Error {
    const error = new Error(message);
    (error as any).code = code;
    return error;
  }
}
```

### Web Plugin with Browser APIs

```typescript
import { WebPlugin } from '@capacitor/core';
import type { GeolocationPluginPlugin, Position } from './definitions';

export class GeolocationPluginWeb extends WebPlugin implements GeolocationPluginPlugin {
  async getCurrentPosition(): Promise<Position> {
    // Check if Geolocation API available
    if (!('geolocation' in navigator)) {
      throw this.unavailable('Geolocation API not available in this browser');
    }

    return new Promise((resolve, reject) => {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          resolve({
            latitude: position.coords.latitude,
            longitude: position.coords.longitude,
            accuracy: position.coords.accuracy,
            altitude: position.coords.altitude,
            altitudeAccuracy: position.coords.altitudeAccuracy,
            heading: position.coords.heading,
            speed: position.coords.speed,
            timestamp: position.timestamp,
          });
        },
        (error) => {
          reject(this.createError('LOCATION_ERROR', error.message));
        },
        {
          enableHighAccuracy: true,
          timeout: 10000,
          maximumAge: 0,
        }
      );
    });
  }

  private createError(code: string, message: string): Error {
    const error = new Error(message);
    (error as any).code = code;
    return error;
  }
}
```

### Web Plugin with Events

```typescript
import { WebPlugin } from '@capacitor/core';
import type {
  BatteryPluginPlugin,
  BatteryInfo,
  PluginListenerHandle
} from './definitions';

export class BatteryPluginWeb extends WebPlugin implements BatteryPluginPlugin {
  private batteryManager: any = null;
  private listeners: Map<string, Set<(info: BatteryInfo) => void>> = new Map();

  async getBatteryStatus(): Promise<BatteryInfo> {
    const battery = await this.getBatteryManager();
    return {
      level: Math.round(battery.level * 100),
      isCharging: battery.charging,
    };
  }

  async addListener(
    eventName: 'batteryChange',
    listenerFunc: (info: BatteryInfo) => void,
  ): Promise<PluginListenerHandle> {
    // Get or create listener set
    if (!this.listeners.has(eventName)) {
      this.listeners.set(eventName, new Set());
    }
    const listenerSet = this.listeners.get(eventName)!;

    // Add listener
    listenerSet.add(listenerFunc);

    // Set up battery event listener if first listener
    if (listenerSet.size === 1) {
      await this.startMonitoring();
    }

    // Return handle for removal
    return {
      remove: async () => {
        listenerSet.delete(listenerFunc);
        if (listenerSet.size === 0) {
          await this.stopMonitoring();
        }
      },
    };
  }

  async removeAllListeners(): Promise<void> {
    this.listeners.clear();
    await this.stopMonitoring();
  }

  private async startMonitoring(): Promise<void> {
    const battery = await this.getBatteryManager();

    const handler = async () => {
      const info = await this.getBatteryStatus();
      this.notifyListeners('batteryChange', info);
    };

    battery.addEventListener('levelchange', handler);
    battery.addEventListener('chargingchange', handler);
  }

  private async stopMonitoring(): Promise<void> {
    if (!this.batteryManager) return;

    // Remove event listeners
    this.batteryManager.removeEventListener('levelchange', this.handleBatteryChange);
    this.batteryManager.removeEventListener('chargingchange', this.handleBatteryChange);
  }

  private handleBatteryChange = async () => {
    const info = await this.getBatteryStatus();
    // Notify all listeners
    const listenerSet = this.listeners.get('batteryChange');
    if (listenerSet) {
      listenerSet.forEach(listener => listener(info));
    }
  };

  private async getBatteryManager(): Promise<any> {
    if (this.batteryManager) {
      return this.batteryManager;
    }

    if ('getBattery' in navigator) {
      this.batteryManager = await (navigator as any).getBattery();
      return this.batteryManager;
    }

    throw this.unavailable('Battery API not available');
  }
}
```

### Mock Implementation (No Web API)

```typescript
import { WebPlugin } from '@capacitor/core';
import type { NfcPluginPlugin, NfcTag } from './definitions';

export class NfcPluginWeb extends WebPlugin implements NfcPluginPlugin {
  async scan(): Promise<NfcTag> {
    // NFC not available on web - provide mock data for testing
    console.warn('NFC not available on web platform. Returning mock data.');

    return {
      id: 'mock-tag-123',
      type: 'NFC_TYPE_MIFARE_CLASSIC',
      data: new Uint8Array([1, 2, 3, 4]),
    };
  }

  async isAvailable(): Promise<{ available: boolean }> {
    // Always return false on web
    return { available: false };
  }
}
```

### Throw When Unavailable

```typescript
import { WebPlugin } from '@capacitor/core';
import type { BiometricPluginPlugin } from './definitions';

export class BiometricPluginWeb extends WebPlugin implements BiometricPluginPlugin {
  async authenticate(): Promise<void> {
    // Biometric auth not available on web
    throw this.unavailable('Biometric authentication is not available on web platform');
  }

  async isAvailable(): Promise<{ available: boolean }> {
    return { available: false };
  }
}
```

---

## 3. Plugin Registration (`src/index.ts`)

### Basic Registration

```typescript
import { registerPlugin } from '@capacitor/core';
import type { MyPluginPlugin } from './definitions';

const MyPlugin = registerPlugin<MyPluginPlugin>('MyPlugin', {
  web: () => import('./web').then(m => new m.MyPluginWeb()),
});

export * from './definitions';
export { MyPlugin };
```

### Registration with Fallback

```typescript
import { registerPlugin } from '@capacitor/core';
import type { MyPluginPlugin } from './definitions';

const MyPlugin = registerPlugin<MyPluginPlugin>('MyPlugin', {
  web: () => import('./web').then(m => new m.MyPluginWeb()),
  // iOS and Android implementations are loaded automatically by Capacitor
});

// Optionally export as default
export default MyPlugin;

// Export types and interfaces
export * from './definitions';

// Named export
export { MyPlugin };
```

---

## 4. Advanced Patterns

### Singleton Pattern

```typescript
// src/index.ts
import { registerPlugin } from '@capacitor/core';
import type { DatabasePluginPlugin } from './definitions';

const DatabasePlugin = registerPlugin<DatabasePluginPlugin>('DatabasePlugin', {
  web: () => import('./web').then(m => new m.DatabasePluginWeb()),
});

// Create singleton instance
class DatabaseService {
  private static instance: DatabaseService;
  private plugin = DatabasePlugin;

  private constructor() {}

  static getInstance(): DatabaseService {
    if (!DatabaseService.instance) {
      DatabaseService.instance = new DatabaseService();
    }
    return DatabaseService.instance;
  }

  async query(sql: string): Promise<any[]> {
    return this.plugin.query({ sql });
  }
}

export const Database = DatabaseService.getInstance();
export * from './definitions';
```

### Typed Error Classes

```typescript
// src/errors.ts
export enum PluginErrorCode {
  UNAVAILABLE = 'UNAVAILABLE',
  PERMISSION_DENIED = 'PERMISSION_DENIED',
  INVALID_PARAMETER = 'INVALID_PARAMETER',
  OPERATION_FAILED = 'OPERATION_FAILED',
  TIMEOUT = 'TIMEOUT',
}

export class PluginError extends Error {
  constructor(
    public code: PluginErrorCode,
    message: string,
    public details?: any
  ) {
    super(message);
    this.name = 'PluginError';
  }
}

// Usage in web.ts
import { PluginError, PluginErrorCode } from './errors';

export class MyPluginWeb extends WebPlugin {
  async method(): Promise<void> {
    throw new PluginError(
      PluginErrorCode.PERMISSION_DENIED,
      'Camera permission not granted',
      { requested: 'camera', status: 'denied' }
    );
  }
}
```

### Helper Utilities

```typescript
// src/utils.ts

/**
 * Validate required parameters
 */
export function validateRequired<T>(
  options: T,
  requiredKeys: (keyof T)[]
): void {
  for (const key of requiredKeys) {
    if (options[key] === undefined || options[key] === null) {
      throw new Error(`INVALID_PARAMETER: ${String(key)} is required`);
    }
  }
}

/**
 * Apply default values to options
 */
export function applyDefaults<T>(
  options: Partial<T>,
  defaults: T
): T {
  return { ...defaults, ...options };
}

/**
 * Debounce function calls
 */
export function debounce<T extends (...args: any[]) => any>(
  func: T,
  wait: number
): (...args: Parameters<T>) => void {
  let timeout: NodeJS.Timeout;
  return (...args: Parameters<T>) => {
    clearTimeout(timeout);
    timeout = setTimeout(() => func(...args), wait);
  };
}

// Usage in web.ts
import { validateRequired, applyDefaults } from './utils';

export class MyPluginWeb extends WebPlugin {
  async method(options: MyOptions): Promise<void> {
    // Validate
    validateRequired(options, ['url', 'method']);

    // Apply defaults
    const opts = applyDefaults(options, {
      timeout: 30000,
      retry: 3,
      followRedirects: true,
    });

    // Proceed with validated options
  }
}
```

---

## 5. Testing

### Unit Tests with Jest

```typescript
// src/__tests__/web.test.ts
import { MyPluginWeb } from '../web';

describe('MyPluginWeb', () => {
  let plugin: MyPluginWeb;

  beforeEach(() => {
    plugin = new MyPluginWeb();
  });

  test('methodName returns expected result', async () => {
    const result = await plugin.methodName({
      param1: 'test',
      param2: 42,
    });

    expect(result.success).toBe(true);
    expect(result.data).toBeDefined();
  });

  test('methodName throws on invalid input', async () => {
    await expect(
      plugin.methodName({ param1: '', param2: 0 })
    ).rejects.toThrow('INVALID_PARAMETER');
  });

  test('methodName validates parameters', async () => {
    await expect(
      plugin.methodName({ param1: null as any })
    ).rejects.toThrow();
  });
});
```

### Mock Browser APIs

```typescript
// src/__tests__/geolocation-web.test.ts
import { GeolocationPluginWeb } from '../web';

// Mock navigator.geolocation
const mockGeolocation = {
  getCurrentPosition: jest.fn(),
  watchPosition: jest.fn(),
};

beforeAll(() => {
  Object.defineProperty(global.navigator, 'geolocation', {
    value: mockGeolocation,
    configurable: true,
  });
});

describe('GeolocationPluginWeb', () => {
  let plugin: GeolocationPluginWeb;

  beforeEach(() => {
    plugin = new GeolocationPluginWeb();
    jest.clearAllMocks();
  });

  test('getCurrentPosition returns position', async () => {
    const mockPosition = {
      coords: {
        latitude: 37.7749,
        longitude: -122.4194,
        accuracy: 10,
        altitude: null,
        altitudeAccuracy: null,
        heading: null,
        speed: null,
      },
      timestamp: Date.now(),
    };

    mockGeolocation.getCurrentPosition.mockImplementation((success) => {
      success(mockPosition);
    });

    const result = await plugin.getCurrentPosition();

    expect(result.latitude).toBe(37.7749);
    expect(result.longitude).toBe(-122.4194);
  });

  test('getCurrentPosition handles error', async () => {
    mockGeolocation.getCurrentPosition.mockImplementation((_, error) => {
      error({ message: 'Permission denied' });
    });

    await expect(plugin.getCurrentPosition()).rejects.toThrow('Permission denied');
  });
});
```

---

## 6. TypeScript Configuration

### tsconfig.json

```json
{
  "compilerOptions": {
    "target": "ES2017",
    "module": "ES2020",
    "lib": ["ES2020", "DOM"],
    "declaration": true,
    "declarationMap": true,
    "outDir": "./dist",
    "rootDir": "./src",
    "strict": true,
    "esModuleInterop": true,
    "skipLibCheck": true,
    "forceConsistentCasingInFileNames": true,
    "moduleResolution": "node",
    "resolveJsonModule": true
  },
  "include": ["src/**/*"],
  "exclude": ["node_modules", "dist", "**/__tests__/**"]
}
```

---

## 7. Build Configuration

### package.json Scripts

```json
{
  "name": "@company/capacitor-myplugin",
  "version": "1.0.0",
  "main": "dist/index.js",
  "module": "dist/index.esm.js",
  "types": "dist/index.d.ts",
  "files": [
    "dist/",
    "ios/",
    "android/"
  ],
  "scripts": {
    "build": "npm run clean && tsc && rollup -c rollup.config.js",
    "clean": "rimraf dist",
    "watch": "tsc --watch",
    "test": "jest",
    "lint": "eslint src/**/*.ts",
    "prepublishOnly": "npm run build"
  },
  "devDependencies": {
    "@capacitor/core": "^6.0.0",
    "@types/jest": "^29.0.0",
    "jest": "^29.0.0",
    "rollup": "^3.0.0",
    "typescript": "^5.0.0"
  },
  "peerDependencies": {
    "@capacitor/core": "^6.0.0"
  }
}
```

---

## Summary

**TypeScript Implementation Checklist**:

- [ ] Define clear interfaces in `definitions.ts`
- [ ] Implement web platform in `web.ts`
- [ ] Register plugin in `index.ts`
- [ ] Add JSDoc comments to all public APIs
- [ ] Validate input parameters
- [ ] Handle errors consistently
- [ ] Write unit tests
- [ ] Configure TypeScript compilation
- [ ] Export types and interfaces

**Remember**: The TypeScript layer is your plugin's public API. Make it type-safe and well-documented!
