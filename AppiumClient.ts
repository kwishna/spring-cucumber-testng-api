/**
 * Production-Grade Mobile Automation Framework
 * TypeScript + WebdriverIO Library
 * 
 * @author AI Assistant
 * @version 2.0.0
 * @description Comprehensive mobile automation framework supporting Android & iOS
 */

import { remote, RemoteOptions, Browser } from 'webdriverio';
import fs from 'fs';
import path from 'path';

// ==================== TYPES & INTERFACES ====================

export interface AppiumConfig {
  hostname?: string;
  port?: number;
  logLevel?: 'trace' | 'debug' | 'info' | 'warn' | 'error' | 'silent';
  capabilities: WebdriverIO.Capabilities;
  connectionRetryTimeout?: number;
  connectionRetryCount?: number;
}

export interface ElementOptions {
  timeout?: number;
  interval?: number;
  reverse?: boolean;
}

export interface ScrollOptions {
  direction?: 'up' | 'down' | 'left' | 'right';
  maxScrolls?: number;
  scrollRatio?: number;
  strategy?: 'touch' | 'uiautomator' | 'predicate';
}

export interface SwipeOptions {
  duration?: number;
  percentage?: number;
  startPercentage?: number;
}

export interface GestureOptions {
  duration?: number;
  x?: number;
  y?: number;
  element?: MobileElement;
  percentage?: number;
}

export interface Coordinate {
  x: number;
  y: number;
}

export interface TouchAction {
  action: 'press' | 'moveTo' | 'tap' | 'wait' | 'longPress' | 'release';
  x?: number;
  y?: number;
  duration?: number;
  element?: any;
}

export enum Platform {
  ANDROID = 'android',
  IOS = 'ios'
}

export enum Locator {
  ID = 'id',
  XPATH = 'xpath',
  ACCESSIBILITY_ID = 'accessibility id',
  CLASS_NAME = 'class name',
  IOS_PREDICATE = '-ios predicate string',
  IOS_CLASS_CHAIN = '-ios class chain',
  ANDROID_UIAUTOMATOR = '-android uiautomator',
  IMAGE = '-image'
}

// ==================== MOBILE ELEMENT ====================

export class MobileElement {
  private element: WebdriverIO.Element;
  private client: AppiumClient;
  private locatorStrategy: string;
  private locatorValue: string;

  constructor(
    element: WebdriverIO.Element,
    client: AppiumClient,
    strategy: string,
    value: string
  ) {
    this.element = element;
    this.client = client;
    this.locatorStrategy = strategy;
    this.locatorValue = value;
  }

  async click(): Promise<void> {
    await this.element.click();
  }

  async tap(): Promise<void> {
    await this.element.click();
  }

  async doubleClick(): Promise<void> {
    await this.element.doubleClick();
  }

  async sendKeys(text: string): Promise<void> {
    await this.element.setValue(text);
  }

  async setText(text: string): Promise<void> {
    await this.element.clearValue();
    await this.element.setValue(text);
  }

  async appendText(text: string): Promise<void> {
    await this.element.addValue(text);
  }

  async clear(): Promise<void> {
    await this.element.clearValue();
  }

  async getText(): Promise<string> {
    return await this.element.getText();
  }

  async getAttribute(attribute: string): Promise<string> {
    return await this.element.getAttribute(attribute);
  }

  async isDisplayed(): Promise<boolean> {
    return await this.element.isDisplayed();
  }

  async isEnabled(): Promise<boolean> {
    return await this.element.isEnabled();
  }

  async isSelected(): Promise<boolean> {
    return await this.element.isSelected();
  }

  async getLocation(): Promise<Coordinate> {
    const location = await this.element.getLocation();
    return { x: location.x, y: location.y };
  }

  async getSize(): Promise<{ width: number; height: number }> {
    return await this.element.getSize();
  }

  async getRect(): Promise<{ x: number; y: number; width: number; height: number }> {
    const location = await this.getLocation();
    const size = await this.getSize();
    return { ...location, ...size };
  }

  async waitForDisplayed(timeout: number = 30000): Promise<boolean> {
    return await this.element.waitForDisplayed({ timeout });
  }

  async waitForEnabled(timeout: number = 30000): Promise<boolean> {
    return await this.element.waitForEnabled({ timeout });
  }

  async waitForExist(timeout: number = 30000): Promise<boolean> {
    return await this.element.waitForExist({ timeout });
  }

  async scrollIntoView(): Promise<void> {
    await this.element.scrollIntoView();
  }

  async longPress(duration: number = 1000): Promise<void> {
    await this.client.longPress({ element: this, duration });
  }

  async swipe(direction: 'up' | 'down' | 'left' | 'right', options?: SwipeOptions): Promise<void> {
    const rect = await this.getRect();
    await this.client.swipeOnElement(this, direction, options);
  }

  async dragAndDropTo(target: MobileElement, duration: number = 1000): Promise<void> {
    await this.client.dragAndDrop(this, target, duration);
  }

  getRawElement(): WebdriverIO.Element {
    return this.element;
  }

  getLocator(): { strategy: string; value: string } {
    return { strategy: this.locatorStrategy, value: this.locatorValue };
  }
}

// ==================== GESTURE HANDLER ====================

export class GestureHandler {
  private client: AppiumClient;

  constructor(client: AppiumClient) {
    this.client = client;
  }

  /**
   * Performs a tap gesture at specified coordinates or on element
   */
  async tap(options: GestureOptions): Promise<void> {
    if (options.element) {
      await options.element.tap();
    } else if (options.x !== undefined && options.y !== undefined) {
      await this.client.getDriver().touchAction({
        action: 'tap',
        x: options.x,
        y: options.y
      });
    } else {
      throw new Error('Either element or coordinates (x, y) must be provided');
    }
  }

  /**
   * Performs a double tap gesture
   */
  async doubleTap(options: GestureOptions): Promise<void> {
    if (options.element) {
      await options.element.doubleClick();
    } else if (options.x !== undefined && options.y !== undefined) {
      await this.performGenericGesture([
        { action: 'tap', x: options.x, y: options.y },
        { action: 'wait', duration: 50 },
        { action: 'tap', x: options.x, y: options.y }
      ]);
    }
  }

  /**
   * Performs a long press gesture
   */
  async longPress(options: GestureOptions): Promise<void> {
    const duration = options.duration || 1000;
    
    if (options.element) {
      const rect = await options.element.getRect();
      const x = rect.x + rect.width / 2;
      const y = rect.y + rect.height / 2;
      await this.performGenericGesture([
        { action: 'press', x, y },
        { action: 'wait', duration },
        { action: 'release' }
      ]);
    } else if (options.x !== undefined && options.y !== undefined) {
      await this.performGenericGesture([
        { action: 'press', x: options.x, y: options.y },
        { action: 'wait', duration },
        { action: 'release' }
      ]);
    }
  }

  /**
   * Performs a swipe gesture
   */
  async swipe(
    startX: number,
    startY: number,
    endX: number,
    endY: number,
    duration: number = 800
  ): Promise<void> {
    await this.performGenericGesture([
      { action: 'press', x: startX, y: startY },
      { action: 'wait', duration: 50 },
      { action: 'moveTo', x: endX, y: endY },
      { action: 'release' }
    ]);
  }

  /**
   * Performs a drag gesture
   */
  async drag(
    startX: number,
    startY: number,
    endX: number,
    endY: number,
    duration: number = 1000
  ): Promise<void> {
    await this.performGenericGesture([
      { action: 'longPress', x: startX, y: startY, duration: 500 },
      { action: 'moveTo', x: endX, y: endY },
      { action: 'release' }
    ]);
  }

  /**
   * Performs a pinch gesture (zoom out)
   */
  async pinch(element?: MobileElement, scale: number = 0.5): Promise<void> {
    const driver = this.client.getDriver();
    const rect = element ? await element.getRect() : await this.client.getWindowSize();
    
    const centerX = element ? rect.x + rect.width / 2 : rect.width / 2;
    const centerY = element ? rect.y + rect.height / 2 : rect.height / 2;
    
    const distance = Math.min(rect.width, rect.height) * 0.4;
    const targetDistance = distance * scale;

    // Finger 1: starts from top, moves towards center
    const finger1Start = { x: centerX, y: centerY - distance };
    const finger1End = { x: centerX, y: centerY - targetDistance };

    // Finger 2: starts from bottom, moves towards center
    const finger2Start = { x: centerX, y: centerY + distance };
    const finger2End = { x: centerX, y: centerY + targetDistance };

    await driver.multiTouchPerform([
      [
        { action: 'press', x: finger1Start.x, y: finger1Start.y },
        { action: 'moveTo', x: finger1End.x, y: finger1End.y },
        { action: 'release' }
      ],
      [
        { action: 'press', x: finger2Start.x, y: finger2Start.y },
        { action: 'moveTo', x: finger2End.x, y: finger2End.y },
        { action: 'release' }
      ]
    ]);
  }

  /**
   * Performs a zoom gesture (pinch out)
   */
  async zoom(element?: MobileElement, scale: number = 2.0): Promise<void> {
    const driver = this.client.getDriver();
    const rect = element ? await element.getRect() : await this.client.getWindowSize();
    
    const centerX = element ? rect.x + rect.width / 2 : rect.width / 2;
    const centerY = element ? rect.y + rect.height / 2 : rect.height / 2;
    
    const startDistance = Math.min(rect.width, rect.height) * 0.1;
    const targetDistance = startDistance * scale;

    // Finger 1: starts near center, moves outward to top
    const finger1Start = { x: centerX, y: centerY - startDistance };
    const finger1End = { x: centerX, y: centerY - targetDistance };

    // Finger 2: starts near center, moves outward to bottom
    const finger2Start = { x: centerX, y: centerY + startDistance };
    const finger2End = { x: centerX, y: centerY + targetDistance };

    await driver.multiTouchPerform([
      [
        { action: 'press', x: finger1Start.x, y: finger1Start.y },
        { action: 'moveTo', x: finger1End.x, y: finger1End.y },
        { action: 'release' }
      ],
      [
        { action: 'press', x: finger2Start.x, y: finger2Start.y },
        { action: 'moveTo', x: finger2End.x, y: finger2End.y },
        { action: 'release' }
      ]
    ]);
  }

  /**
   * Generic gesture performer - performs any complex finger action
   * This is the most powerful method that can handle any touch sequence
   * 
   * @param actions Array of touch actions to perform
   * @example
   * await gestureHandler.performGenericGesture([
   *   { action: 'press', x: 100, y: 200 },
   *   { action: 'wait', duration: 500 },
   *   { action: 'moveTo', x: 300, y: 400 },
   *   { action: 'release' }
   * ]);
   */
  async performGenericGesture(actions: TouchAction[]): Promise<void> {
    const driver = this.client.getDriver();
    
    for (const action of actions) {
      switch (action.action) {
        case 'press':
        case 'tap':
        case 'longPress':
        case 'moveTo':
          await driver.touchAction(action);
          break;
        case 'wait':
          if (action.duration) {
            await this.client.sleep(action.duration);
          }
          break;
        case 'release':
          await driver.touchAction({ action: 'release' });
          break;
      }
    }
  }

  /**
   * Multi-finger generic gesture performer
   * Allows complex multi-touch gestures like custom pinch, zoom, rotate, etc.
   * 
   * @param fingerActions Array of action sequences, one per finger
   * @example
   * await gestureHandler.performMultiFingerGesture([
   *   [ // Finger 1
   *     { action: 'press', x: 100, y: 100 },
   *     { action: 'moveTo', x: 200, y: 200 },
   *     { action: 'release' }
   *   ],
   *   [ // Finger 2
   *     { action: 'press', x: 300, y: 100 },
   *     { action: 'moveTo', x: 400, y: 200 },
   *     { action: 'release' }
   *   ]
   * ]);
   */
  async performMultiFingerGesture(fingerActions: TouchAction[][]): Promise<void> {
    const driver = this.client.getDriver();
    await driver.multiTouchPerform(fingerActions);
  }

  /**
   * Performs a rotation gesture (two-finger rotate)
   */
  async rotate(element?: MobileElement, angle: number = 90, duration: number = 1000): Promise<void> {
    const rect = element ? await element.getRect() : await this.client.getWindowSize();
    const centerX = element ? rect.x + rect.width / 2 : rect.width / 2;
    const centerY = element ? rect.y + rect.height / 2 : rect.height / 2;
    const radius = Math.min(rect.width, rect.height) * 0.3;

    const angleRad = (angle * Math.PI) / 180;

    // Finger 1: starts at top, rotates clockwise
    const finger1StartX = centerX;
    const finger1StartY = centerY - radius;
    const finger1EndX = centerX + radius * Math.sin(angleRad);
    const finger1EndY = centerY - radius * Math.cos(angleRad);

    // Finger 2: starts at bottom, rotates clockwise
    const finger2StartX = centerX;
    const finger2StartY = centerY + radius;
    const finger2EndX = centerX - radius * Math.sin(angleRad);
    const finger2EndY = centerY + radius * Math.cos(angleRad);

    await this.performMultiFingerGesture([
      [
        { action: 'press', x: finger1StartX, y: finger1StartY },
        { action: 'moveTo', x: finger1EndX, y: finger1EndY },
        { action: 'release' }
      ],
      [
        { action: 'press', x: finger2StartX, y: finger2StartY },
        { action: 'moveTo', x: finger2EndX, y: finger2EndY },
        { action: 'release' }
      ]
    ]);
  }

  /**
   * Performs a three-finger swipe (useful for iOS gestures)
   */
  async threeFingerSwipe(direction: 'up' | 'down' | 'left' | 'right', duration: number = 500): Promise<void> {
    const size = await this.client.getWindowSize();
    const startY = size.height * 0.5;
    const spacing = size.width * 0.15;
    
    let startX: number, endX: number, endY: number;

    switch (direction) {
      case 'up':
        startX = size.width * 0.5;
        endX = startX;
        endY = size.height * 0.2;
        break;
      case 'down':
        startX = size.width * 0.5;
        endX = startX;
        endY = size.height * 0.8;
        break;
      case 'left':
        startX = size.width * 0.8;
        endX = size.width * 0.2;
        endY = startY;
        break;
      case 'right':
        startX = size.width * 0.2;
        endX = size.width * 0.8;
        endY = startY;
        break;
    }

    await this.performMultiFingerGesture([
      [
        { action: 'press', x: startX - spacing, y: startY },
        { action: 'moveTo', x: endX - spacing, y: endY },
        { action: 'release' }
      ],
      [
        { action: 'press', x: startX, y: startY },
        { action: 'moveTo', x: endX, y: endY },
        { action: 'release' }
      ],
      [
        { action: 'press', x: startX + spacing, y: startY },
        { action: 'moveTo', x: endX + spacing, y: endY },
        { action: 'release' }
      ]
    ]);
  }
}

// ==================== ACTION CHAIN ====================

export class ActionChain {
  private actions: Array<() => Promise<void>> = [];
  private client: AppiumClient;

  constructor(client: AppiumClient) {
    this.client = client;
  }

  /**
   * Adds a tap action to the chain
   */
  tap(element: MobileElement): ActionChain {
    this.actions.push(async () => await element.tap());
    return this;
  }

  /**
   * Adds a click action to the chain
   */
  click(element: MobileElement): ActionChain {
    this.actions.push(async () => await element.click());
    return this;
  }

  /**
   * Adds a sendKeys action to the chain
   */
  sendKeys(element: MobileElement, text: string): ActionChain {
    this.actions.push(async () => await element.sendKeys(text));
    return this;
  }

  /**
   * Adds a setText action to the chain
   */
  setText(element: MobileElement, text: string): ActionChain {
    this.actions.push(async () => await element.setText(text));
    return this;
  }

  /**
   * Adds a swipe action to the chain
   */
  swipe(direction: 'up' | 'down' | 'left' | 'right', options?: SwipeOptions): ActionChain {
    this.actions.push(async () => await this.client.swipe(direction, options));
    return this;
  }

  /**
   * Adds a scroll action to the chain
   */
  scroll(direction: 'up' | 'down' | 'left' | 'right', options?: ScrollOptions): ActionChain {
    this.actions.push(async () => await this.client.scroll(direction, options));
    return this;
  }

  /**
   * Adds a long press action to the chain
   */
  longPress(options: GestureOptions): ActionChain {
    this.actions.push(async () => await this.client.longPress(options));
    return this;
  }

  /**
   * Adds a wait/sleep action to the chain
   */
  wait(milliseconds: number): ActionChain {
    this.actions.push(async () => await this.client.sleep(milliseconds));
    return this;
  }

  /**
   * Adds a waitForElement action to the chain
   */
  waitForElement(locator: string, strategy: Locator = Locator.XPATH, timeout: number = 30000): ActionChain {
    this.actions.push(async () => {
      await this.client.waitForElement(locator, strategy, timeout);
    });
    return this;
  }

  /**
   * Adds a hideKeyboard action to the chain
   */
  hideKeyboard(): ActionChain {
    this.actions.push(async () => await this.client.hideKeyboard());
    return this;
  }

  /**
   * Adds a screenshot action to the chain
   */
  takeScreenshot(filepath: string): ActionChain {
    this.actions.push(async () => await this.client.takeScreenshot(filepath));
    return this;
  }

  /**
   * Adds a custom action to the chain
   */
  custom(action: () => Promise<void>): ActionChain {
    this.actions.push(action);
    return this;
  }

  /**
   * Adds a conditional action to the chain
   */
  if(condition: () => Promise<boolean>, thenAction: (chain: ActionChain) => ActionChain): ActionChain {
    this.actions.push(async () => {
      if (await condition()) {
        const subChain = new ActionChain(this.client);
        thenAction(subChain);
        await subChain.perform();
      }
    });
    return this;
  }

  /**
   * Adds a loop action to the chain
   */
  repeat(times: number, action: (chain: ActionChain) => ActionChain): ActionChain {
    this.actions.push(async () => {
      for (let i = 0; i < times; i++) {
        const subChain = new ActionChain(this.client);
        action(subChain);
        await subChain.perform();
      }
    });
    return this;
  }

  /**
   * Executes all actions in the chain
   */
  async perform(): Promise<void> {
    for (const action of this.actions) {
      await action();
    }
    this.actions = []; // Clear actions after execution
  }

  /**
   * Clears all actions from the chain
   */
  clear(): ActionChain {
    this.actions = [];
    return this;
  }

  /**
   * Returns the number of actions in the chain
   */
  length(): number {
    return this.actions.length;
  }
}

// ==================== MAIN APPIUM CLIENT ====================

export class AppiumClient {
  private driver!: Browser;
  private config: AppiumConfig;
  private platform!: Platform;
  private gestureHandler!: GestureHandler;
  private defaultTimeout: number = 30000;
  private defaultInterval: number = 500;

  constructor(config: AppiumConfig) {
    this.config = {
      hostname: config.hostname || process.env.APPIUM_HOST || 'localhost',
      port: config.port || parseInt(process.env.APPIUM_PORT || '4723', 10),
      logLevel: config.logLevel || 'info',
      connectionRetryTimeout: config.connectionRetryTimeout || 120000,
      connectionRetryCount: config.connectionRetryCount || 3,
      capabilities: config.capabilities
    };
  }

  // ==================== SESSION MANAGEMENT ====================

  /**
   * Initializes the Appium driver and starts a session
   */
  async init(): Promise<void> {
    const wdOpts: RemoteOptions = {
      hostname: this.config.hostname,
      port: this.config.port,
      logLevel: this.config.logLevel,
      connectionRetryTimeout: this.config.connectionRetryTimeout,
      connectionRetryCount: this.config.connectionRetryCount,
      capabilities: this.config.capabilities
    };

    this.driver = await remote(wdOpts);
    
    // Detect platform
    const platformName = this.config.capabilities.platformName?.toString().toLowerCase();
    this.platform = platformName === 'ios' ? Platform.IOS : Platform.ANDROID;
    
    // Initialize gesture handler
    this.gestureHandler = new GestureHandler(this);
    
    console.log(`[AppiumClient] Session started on ${this.platform}`);
  }

  /**
   * Quits the driver and ends the session
   */
  async quit(): Promise<void> {
    if (this.driver) {
      await this.driver.deleteSession();
      console.log('[AppiumClient] Session ended');
    }
  }

  /**
   * Gets the driver instance
   */
  getDriver(): Browser {
    return this.driver;
  }

  /**
   * Gets the gesture handler
   */
  getGestureHandler(): GestureHandler {
    return this.gestureHandler;
  }

  /**
   * Gets the platform
   */
  getPlatform(): Platform {
    return this.platform;
  }

  /**
   * Creates a new action chain
   */
  createActionChain(): ActionChain {
    return new ActionChain(this);
  }

  // ==================== ELEMENT FINDING ====================

  /**
   * Finds a single element
   */
  async findElement(locator: string, strategy: Locator = Locator.XPATH): Promise<MobileElement> {
    const element = await this.driver.$(this.buildSelector(locator, strategy));
    return new MobileElement(element, this, strategy, locator);
  }

  /**
   * Finds multiple elements
   */
  async findElements(locator: string, strategy: Locator = Locator.XPATH): Promise<MobileElement[]> {
    const elements = await this.driver.$$(this.buildSelector(locator, strategy));
    return elements.map((el, idx) => new MobileElement(el, this, strategy, `${locator}[${idx}]`));
  }

  /**
   * Finds element by ID
   */
  async findById(id: string): Promise<MobileElement> {
    return await this.findElement(id, Locator.ID);
  }

  /**
   * Finds element by accessibility ID
   */
  async findByAccessibilityId(accessibilityId: string): Promise<MobileElement> {
    return await this.findElement(accessibilityId, Locator.ACCESSIBILITY_ID);
  }

  /**
   * Finds element by XPath
   */
  async findByXPath(xpath: string): Promise<MobileElement> {
    return await this.findElement(xpath, Locator.XPATH);
  }

  /**
   * Finds element by class name
   */
  async findByClassName(className: string): Promise<MobileElement> {
    return await this.findElement(className, Locator.CLASS_NAME);
  }

  /**
   * Finds element by iOS predicate string (iOS only)
   */
  async findByIOSPredicate(predicate: string): Promise<MobileElement> {
    return await this.findElement(predicate, Locator.IOS_PREDICATE);
  }

  /**
   * Finds element by iOS class chain (iOS only)
   */
  async findByIOSClassChain(classChain: string): Promise<MobileElement> {
    return await this.findElement(classChain, Locator.IOS_CLASS_CHAIN);
  }

  /**
   * Finds element by Android UIAutomator (Android only)
   */
  async findByAndroidUIAutomator(uiAutomator: string): Promise<MobileElement> {
    return await this.findElement(uiAutomator, Locator.ANDROID_UIAUTOMATOR);
  }

  /**
   * Finds element by image
   */
  async findByImage(base64Image: string): Promise<MobileElement> {
    return await this.findElement(base64Image, Locator.IMAGE);
  }

  /**
   * Builds selector string for WebdriverIO
   */
  private buildSelector(locator: string, strategy: Locator): string {
    switch (strategy) {
      case Locator.ID:
        return `id=${locator}`;
      case Locator.XPATH:
        return locator;
      case Locator.ACCESSIBILITY_ID:
        return `~${locator}`;
      case Locator.CLASS_NAME:
        return `.${locator}`;
      case Locator.IOS_PREDICATE:
        return `-ios predicate string:${locator}`;
      case Locator.IOS_CLASS_CHAIN:
        return `-ios class chain:${locator}`;
      case Locator.ANDROID_UIAUTOMATOR:
        return `android=${locator}`;
      case Locator.IMAGE:
        return `-image:${locator}`;
      default:
        return locator;
    }
  }

  // ==================== WAIT METHODS ====================

  /**
   * Waits for an element to exist
   */
  async waitForElement(
    locator: string,
    strategy: Locator = Locator.XPATH,
    timeout: number = this.defaultTimeout
  ): Promise<MobileElement> {
    const element = await this.driver.$(this.buildSelector(locator, strategy));
    await element.waitForExist({ timeout });
    return new MobileElement(element, this, strategy, locator);
  }

  /**
   * Waits for an element to be displayed
   */
  async waitForElementDisplayed(
    locator: string,
    strategy: Locator = Locator.XPATH,
    timeout: number = this.defaultTimeout
  ): Promise<MobileElement> {
    const element = await this.driver.$(this.buildSelector(locator, strategy));
    await element.waitForDisplayed({ timeout });
    return new MobileElement(element, this, strategy, locator);
  }

  /**
   * Waits for an element to be enabled
   */
  async waitForElementEnabled(
    locator: string,
    strategy: Locator = Locator.XPATH,
    timeout: number = this.defaultTimeout
  ): Promise<MobileElement> {
    const element = await this.driver.$(this.buildSelector(locator, strategy));
    await element.waitForEnabled({ timeout });
    return new MobileElement(element, this, strategy, locator);
  }

  /**
   * Waits for an element to disappear
   */
  async waitForElementNotDisplayed(
    locator: string,
    strategy: Locator = Locator.XPATH,
    timeout: number = this.defaultTimeout
  ): Promise<void> {
    const element = await this.driver.$(this.buildSelector(locator, strategy));
    await element.waitForDisplayed({ timeout, reverse: true });
  }

  /**
   * Waits for a condition to be true
   */
  async waitUntil(
    condition: () => Promise<boolean>,
    timeout: number = this.defaultTimeout,
    interval: number = this.defaultInterval,
    timeoutMsg?: string
  ): Promise<void> {
    await this.driver.waitUntil(condition, {
      timeout,
      interval,
      timeoutMsg: timeoutMsg || 'Condition was not met within timeout'
    });
  }

  /**
   * Sleeps for specified milliseconds
   */
  async sleep(milliseconds: number): Promise<void> {
    await this.driver.pause(milliseconds);
  }

  // ==================== GESTURES ====================

  /**
   * Performs a tap gesture at coordinates or on element
   */
  async tap(options: GestureOptions): Promise<void> {
    await this.gestureHandler.tap(options);
  }

  /**
   * Performs a double tap gesture
   */
  async doubleTap(options: GestureOptions): Promise<void> {
    await this.gestureHandler.doubleTap(options);
  }

  /**
   * Performs a long press gesture
   */
  async longPress(options: GestureOptions): Promise<void> {
    await this.gestureHandler.longPress(options);
  }

  /**
   * Swipes in a direction
   */
  async swipe(direction: 'up' | 'down' | 'left' | 'right', options?: SwipeOptions): Promise<void> {
    const size = await this.getWindowSize();
    const duration = options?.duration || 800;
    const percentage = options?.percentage || 0.75;
    const startPercentage = options?.startPercentage || 0.5;

    let startX: number, startY: number, endX: number, endY: number;

    switch (direction) {
      case 'up':
        startX = size.width * startPercentage;
        startY = size.height * (1 - percentage);
        endX = startX;
        endY = size.height * percentage;
        break;
      case 'down':
        startX = size.width * startPercentage;
        startY = size.height * percentage;
        endX = startX;
        endY = size.height * (1 - percentage);
        break;
      case 'left':
        startX = size.width * (1 - percentage);
        startY = size.height * startPercentage;
        endX = size.width * percentage;
        endY = startY;
        break;
      case 'right':
        startX = size.width * percentage;
        startY = size.height * startPercentage;
        endX = size.width * (1 - percentage);
        endY = startY;
        break;
    }

    await this.gestureHandler.swipe(startX, startY, endX, endY, duration);
  }

  /**
   * Swipes on a specific element
   */
  async swipeOnElement(
    element: MobileElement,
    direction: 'up' | 'down' | 'left' | 'right',
    options?: SwipeOptions
  ): Promise<void> {
    const rect = await element.getRect();
    const duration = options?.duration || 800;
    const percentage = options?.percentage || 0.75;

    let startX: number, startY: number, endX: number, endY: number;
    const centerX = rect.x + rect.width / 2;
    const centerY = rect.y + rect.height / 2;

    switch (direction) {
      case 'up':
        startX = centerX;
        startY = rect.y + rect.height * (1 - percentage);
        endX = centerX;
        endY = rect.y + rect.height * (1 - percentage) * 0.2;
        break;
      case 'down':
        startX = centerX;
        startY = rect.y + rect.height * percentage * 0.2;
        endX = centerX;
        endY = rect.y + rect.height * percentage;
        break;
      case 'left':
        startX = rect.x + rect.width * (1 - percentage);
        startY = centerY;
        endX = rect.x + rect.width * (1 - percentage) * 0.2;
        endY = centerY;
        break;
      case 'right':
        startX = rect.x + rect.width * percentage * 0.2;
        startY = centerY;
        endX = rect.x + rect.width * percentage;
        endY = centerY;
        break;
    }

    await this.gestureHandler.swipe(startX, startY, endX, endY, duration);
  }

  /**
   * Scrolls in a direction
   */
  async scroll(direction: 'up' | 'down' | 'left' | 'right', options?: ScrollOptions): Promise<void> {
    const maxScrolls = options?.maxScrolls || 10;
    const scrollRatio = options?.scrollRatio || 0.5;
    
    for (let i = 0; i < maxScrolls; i++) {
      await this.swipe(direction, { percentage: scrollRatio, duration: 600 });
      await this.sleep(300);
    }
  }

  /**
   * Scrolls to an element
   */
  async scrollToElement(
    locator: string,
    strategy: Locator = Locator.XPATH,
    options?: ScrollOptions
  ): Promise<MobileElement> {
    const direction = options?.direction || 'down';
    const maxScrolls = options?.maxScrolls || 10;
    const scrollRatio = options?.scrollRatio || 0.5;

    for (let i = 0; i < maxScrolls; i++) {
      try {
        const element = await this.findElement(locator, strategy);
        if (await element.isDisplayed()) {
          return element;
        }
      } catch (e) {
        // Element not found, continue scrolling
      }

      await this.swipe(direction, { percentage: scrollRatio, duration: 600 });
      await this.sleep(300);
    }

    throw new Error(`Element not found after ${maxScrolls} scrolls`);
  }

  /**
   * Scrolls to text
   */
  async scrollToText(text: string, options?: ScrollOptions): Promise<MobileElement> {
    const xpath = this.platform === Platform.ANDROID
      ? `//*[@text='${text}']`
      : `//*[@label='${text}' or @value='${text}']`;
    return await this.scrollToElement(xpath, Locator.XPATH, options);
  }

  /**
   * Performs drag and drop
   */
  async dragAndDrop(source: MobileElement, target: MobileElement, duration: number = 1000): Promise<void> {
    const sourceRect = await source.getRect();
    const targetRect = await target.getRect();

    const sourceX = sourceRect.x + sourceRect.width / 2;
    const sourceY = sourceRect.y + sourceRect.height / 2;
    const targetX = targetRect.x + targetRect.width / 2;
    const targetY = targetRect.y + targetRect.height / 2;

    await this.gestureHandler.drag(sourceX, sourceY, targetX, targetY, duration);
  }

  /**
   * Performs pinch gesture (zoom out)
   */
  async pinch(element?: MobileElement, scale: number = 0.5): Promise<void> {
    await this.gestureHandler.pinch(element, scale);
  }

  /**
   * Performs zoom gesture (pinch out)
   */
  async zoom(element?: MobileElement, scale: number = 2.0): Promise<void> {
    await this.gestureHandler.zoom(element, scale);
  }

  /**
   * Performs rotation gesture
   */
  async rotate(element?: MobileElement, angle: number = 90, duration: number = 1000): Promise<void> {
    await this.gestureHandler.rotate(element, angle, duration);
  }

  /**
   * Performs three-finger swipe
   */
  async threeFingerSwipe(direction: 'up' | 'down' | 'left' | 'right', duration: number = 500): Promise<void> {
    await this.gestureHandler.threeFingerSwipe(direction, duration);
  }

  /**
   * Generic gesture performer for complex actions
   */
  async performGesture(actions: TouchAction[]): Promise<void> {
    await this.gestureHandler.performGenericGesture(actions);
  }

  /**
   * Multi-finger gesture performer
   */
  async performMultiFingerGesture(fingerActions: TouchAction[][]): Promise<void> {
    await this.gestureHandler.performMultiFingerGesture(fingerActions);
  }

  // ==================== DEVICE INTERACTIONS ====================

  /**
   * Hides the keyboard
   */
  async hideKeyboard(): Promise<void> {
    try {
      if (this.platform === Platform.IOS) {
        await this.driver.hideKeyboard('pressKey', 'Done');
      } else {
        await this.driver.hideKeyboard();
      }
    } catch (e) {
      console.log('[AppiumClient] Keyboard already hidden or not present');
    }
  }

  /**
   * Checks if keyboard is shown
   */
  async isKeyboardShown(): Promise<boolean> {
    return await this.driver.isKeyboardShown();
  }

  /**
   * Locks the device
   */
  async lockDevice(seconds?: number): Promise<void> {
    if (this.platform === Platform.ANDROID) {
      await this.driver.lock(seconds);
    } else {
      await this.driver.lock();
      if (seconds) {
        await this.sleep(seconds * 1000);
        await this.driver.unlock();
      }
    }
  }

  /**
   * Unlocks the device
   */
  async unlockDevice(): Promise<void> {
    await this.driver.unlock();
  }

  /**
   * Checks if device is locked
   */
  async isDeviceLocked(): Promise<boolean> {
    return await this.driver.isLocked();
  }

  /**
   * Rotates the device
   */
  async rotate(orientation: 'LANDSCAPE' | 'PORTRAIT'): Promise<void> {
    await this.driver.setOrientation(orientation);
  }

  /**
   * Gets current orientation
   */
  async getOrientation(): Promise<string> {
    return await this.driver.getOrientation();
  }

  /**
   * Shakes the device
   */
  async shake(): Promise<void> {
    await this.driver.execute('mobile: shake', {});
  }

  /**
   * Presses device key
   */
  async pressKey(keyCode: number): Promise<void> {
    if (this.platform === Platform.ANDROID) {
      await this.driver.pressKeyCode(keyCode);
    }
  }

  /**
   * Presses back button (Android)
   */
  async pressBack(): Promise<void> {
    if (this.platform === Platform.ANDROID) {
      await this.driver.back();
    }
  }

  /**
   * Opens notifications (Android)
   */
  async openNotifications(): Promise<void> {
    if (this.platform === Platform.ANDROID) {
      await this.driver.openNotifications();
    }
  }

  /**
   * Gets device time
   */
  async getDeviceTime(format?: string): Promise<string> {
    return await this.driver.getDeviceTime(format);
  }

  /**
   * Gets window size
   */
  async getWindowSize(): Promise<{ width: number; height: number }> {
    const size = await this.driver.getWindowSize();
    return { width: size.width, height: size.height };
  }

  // ==================== APP MANAGEMENT ====================

  /**
   * Launches the app
   */
  async launchApp(): Promise<void> {
    await this.driver.execute('mobile: launchApp', {});
  }

  /**
   * Closes the app
   */
  async closeApp(): Promise<void> {
    await this.driver.closeApp();
  }

  /**
   * Resets the app
   */
  async resetApp(): Promise<void> {
    await this.driver.reset();
  }

  /**
   * Terminates the app
   */
  async terminateApp(bundleId: string): Promise<void> {
    await this.driver.execute('mobile: terminateApp', { bundleId });
  }

  /**
   * Activates the app
   */
  async activateApp(bundleId: string): Promise<void> {
    await this.driver.execute('mobile: activateApp', { bundleId });
  }

  /**
   * Checks if app is installed
   */
  async isAppInstalled(bundleId: string): Promise<boolean> {
    return await this.driver.isAppInstalled(bundleId);
  }

  /**
   * Installs app
   */
  async installApp(appPath: string): Promise<void> {
    await this.driver.installApp(appPath);
  }

  /**
   * Removes app
   */
  async removeApp(bundleId: string): Promise<void> {
    await this.driver.removeApp(bundleId);
  }

  /**
   * Runs app in background
   */
  async runAppInBackground(seconds: number = 5): Promise<void> {
    await this.driver.background(seconds);
  }

  // ==================== CONTEXT MANAGEMENT ====================

  /**
   * Gets current context
   */
  async getCurrentContext(): Promise<string> {
    return await this.driver.getContext();
  }

  /**
   * Gets all contexts
   */
  async getAllContexts(): Promise<string[]> {
    return await this.driver.getContexts();
  }

  /**
   * Switches to context
   */
  async switchToContext(context: string): Promise<void> {
    await this.driver.switchContext(context);
  }

  /**
   * Switches to native context
   */
  async switchToNativeContext(): Promise<void> {
    await this.driver.switchContext('NATIVE_APP');
  }

  /**
   * Switches to webview context
   */
  async switchToWebviewContext(index: number = 0): Promise<void> {
    const contexts = await this.getAllContexts();
    const webviews = contexts.filter(ctx => ctx.includes('WEBVIEW'));
    
    if (webviews.length === 0) {
      throw new Error('No webview context found');
    }

    await this.driver.switchContext(webviews[index]);
  }

  // ==================== CLIPBOARD ====================

  /**
   * Gets clipboard text
   */
  async getClipboard(): Promise<string> {
    return await this.driver.execute('mobile: getClipboard', {});
  }

  /**
   * Sets clipboard text
   */
  async setClipboard(text: string): Promise<void> {
    await this.driver.execute('mobile: setClipboard', { content: text });
  }

  // ==================== SCREENSHOTS & RECORDING ====================

  /**
   * Takes a screenshot
   */
  async takeScreenshot(filepath?: string): Promise<string> {
    const screenshot = await this.driver.takeScreenshot();
    
    if (filepath) {
      const dir = path.dirname(filepath);
      if (!fs.existsSync(dir)) {
        fs.mkdirSync(dir, { recursive: true });
      }
      fs.writeFileSync(filepath, screenshot, 'base64');
      console.log(`[AppiumClient] Screenshot saved to: ${filepath}`);
    }
    
    return screenshot;
  }

  /**
   * Starts screen recording
   */
  async startRecording(options?: {
    videoSize?: string;
    timeLimit?: string;
    bitRate?: string;
  }): Promise<void> {
    await this.driver.startRecordingScreen(options);
  }

  /**
   * Stops screen recording
   */
  async stopRecording(filepath?: string): Promise<string> {
    const video = await this.driver.stopRecordingScreen();
    
    if (filepath) {
      const dir = path.dirname(filepath);
      if (!fs.existsSync(dir)) {
        fs.mkdirSync(dir, { recursive: true });
      }
      fs.writeFileSync(filepath, video, 'base64');
      console.log(`[AppiumClient] Video saved to: ${filepath}`);
    }
    
    return video;
  }

  // ==================== UTILITY METHODS ====================

  /**
   * Gets page source
   */
  async getPageSource(): Promise<string> {
    return await this.driver.getPageSource();
  }

  /**
   * Executes mobile command
   */
  async executeMobile(command: string, params: any = {}): Promise<any> {
    return await this.driver.execute(`mobile: ${command}`, params);
  }

  /**
   * Executes script
   */
  async executeScript(script: string, args?: any[]): Promise<any> {
    return await this.driver.execute(script, args);
  }

  /**
   * Gets session ID
   */
  getSessionId(): string {
    return this.driver.sessionId;
  }

  /**
   * Gets session capabilities
   */
  async getCapabilities(): Promise<WebdriverIO.Capabilities> {
    return await this.driver.capabilities;
  }

  /**
   * Sets implicit wait timeout
   */
  setImplicitTimeout(timeout: number): void {
    this.defaultTimeout = timeout;
  }

  /**
   * Gets implicit wait timeout
   */
  getImplicitTimeout(): number {
    return this.defaultTimeout;
  }

  // ==================== ADVANCED ANDROID METHODS ====================

  /**
   * Starts Android activity
   */
  async startActivity(appPackage: string, appActivity: string): Promise<void> {
    if (this.platform === Platform.ANDROID) {
      await this.driver.startActivity(appPackage, appActivity);
    }
  }

  /**
   * Gets current Android activity
   */
  async getCurrentActivity(): Promise<string> {
    if (this.platform === Platform.ANDROID) {
      return await this.driver.getCurrentActivity();
    }
    throw new Error('getCurrentActivity is only available on Android');
  }

  /**
   * Gets current Android package
   */
  async getCurrentPackage(): Promise<string> {
    if (this.platform === Platform.ANDROID) {
      return await this.driver.getCurrentPackage();
    }
    throw new Error('getCurrentPackage is only available on Android');
  }

  /**
   * Toggles WiFi (Android)
   */
  async toggleWiFi(): Promise<void> {
    if (this.platform === Platform.ANDROID) {
      await this.driver.toggleWiFi();
    }
  }

  /**
   * Toggles airplane mode (Android)
   */
  async toggleAirplaneMode(): Promise<void> {
    if (this.platform === Platform.ANDROID) {
      await this.driver.toggleAirplaneMode();
    }
  }

  /**
   * Toggles location services (Android)
   */
  async toggleLocationServices(): Promise<void> {
    if (this.platform === Platform.ANDROID) {
      await this.driver.toggleLocationServices();
    }
  }

  // ==================== ADVANCED iOS METHODS ====================

  /**
   * Performs iOS touch ID authentication
   */
  async performTouchID(match: boolean = true): Promise<void> {
    if (this.platform === Platform.IOS) {
      await this.driver.touchId(match);
    }
  }

  /**
   * Performs iOS face ID authentication
   */
  async performFaceID(match: boolean = true): Promise<void> {
    if (this.platform === Platform.IOS) {
      await this.driver.execute('mobile: enrollBiometric', { isEnabled: true });
      await this.driver.execute('mobile: sendBiometricMatch', { type: 'faceId', match });
    }
  }

  /**
   * Gets iOS battery info
   */
  async getBatteryInfo(): Promise<{ level: number; state: string }> {
    if (this.platform === Platform.IOS) {
      const info = await this.driver.execute('mobile: batteryInfo', {});
      return info as { level: number; state: string };
    }
    throw new Error('getBatteryInfo is only available on iOS');
  }

  // ==================== DEBUGGING & LOGGING ====================

  /**
   * Logs element hierarchy
   */
  async logElementHierarchy(): Promise<void> {
    const source = await this.getPageSource();
    console.log('[AppiumClient] Page Source:');
    console.log(source);
  }

  /**
   * Logs current contexts
   */
  async logContexts(): Promise<void> {
    const contexts = await this.getAllContexts();
    const current = await this.getCurrentContext();
    console.log('[AppiumClient] Available contexts:', contexts);
    console.log('[AppiumClient] Current context:', current);
  }

  /**
   * Logs device info
   */
  async logDeviceInfo(): Promise<void> {
    const caps = await this.getCapabilities();
    const orientation = await this.getOrientation();
    const size = await this.getWindowSize();
    
    console.log('[AppiumClient] Device Info:');
    console.log('  Platform:', this.platform);
    console.log('  Platform Version:', caps.platformVersion);
    console.log('  Device Name:', caps.deviceName);
    console.log('  Orientation:', orientation);
    console.log('  Screen Size:', `${size.width}x${size.height}`);
  }
}

// ==================== EXPORTS ====================

export default AppiumClient;

// Example usage:
/*
import AppiumClient, { Platform, Locator } from './AppiumClient';

const capabilities = {
  platformName: 'Android',
  'appium:deviceName': 'Pixel 5',
  'appium:automationName': 'UiAutomator2',
  'appium:app': '/path/to/app.apk',
  'appium:noReset': true
};

const client = new AppiumClient({
  hostname: 'localhost',
  port: 4723,
  logLevel: 'info',
  capabilities
});

async function testMobileApp() {
  try {
    // Initialize session
    await client.init();
    
    // Basic element interaction
    const loginButton = await client.findById('login_button');
    await loginButton.click();
    
    // Wait for element
    const welcomeText = await client.waitForElement('//android.widget.TextView[@text="Welcome"]');
    const text = await welcomeText.getText();
    console.log('Welcome message:', text);
    
    // Gestures
    await client.swipe('up', { duration: 1000, percentage: 0.8 });
    await client.longPress({ x: 200, y: 400, duration: 1500 });
    await client.zoom(undefined, 2.0);
    await client.pinch(undefined, 0.5);
    
    // Action chaining
    await client.createActionChain()
      .tap(loginButton)
      .wait(1000)
      .sendKeys(await client.findById('username'), 'testuser')
      .sendKeys(await client.findById('password'), 'testpass')
      .tap(await client.findById('submit'))
      .waitForElement('//android.widget.TextView[@text="Dashboard"]')
      .takeScreenshot('./screenshots/dashboard.png')
      .perform();
    
    // Complex custom gesture
    await client.performGesture([
      { action: 'press', x: 100, y: 200 },
      { action: 'wait', duration: 500 },
      { action: 'moveTo', x: 300, y: 200 },
      { action: 'moveTo', x: 300, y: 400 },
      { action: 'moveTo', x: 100, y: 400 },
      { action: 'moveTo', x: 100, y: 200 },
      { action: 'release' }
    ]);
    
    // Multi-finger gesture (custom pinch)
    await client.performMultiFingerGesture([
      [ // Finger 1
        { action: 'press', x: 200, y: 300 },
        { action: 'moveTo', x: 100, y: 300 },
        { action: 'release' }
      ],
      [ // Finger 2
        { action: 'press', x: 400, y: 300 },
        { action: 'moveTo', x: 500, y: 300 },
        { action: 'release' }
      ]
    ]);
    
    // Scroll to element
    const profileButton = await client.scrollToText('Profile Settings');
    await profileButton.tap();
    
    // Take screenshot
    await client.takeScreenshot('./screenshots/test.png');
    
    // App management
    await client.runAppInBackground(3);
    await client.launchApp();
    
  } finally {
    await client.quit();
  }
}

testMobileApp().catch(console.error);
*/

