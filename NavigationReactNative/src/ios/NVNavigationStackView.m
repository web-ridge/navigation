#import "NVNavigationStackView.h"
#import "NVSceneView.h"
#import "NVSceneController.h"
#import "NVNavigationBarView.h"

#import <UIKit/UIKit.h>
#import <React/RCTBridge.h>
#import <React/RCTI18nUtil.h>
#import <React/RCTUIManager.h>
#import <React/RCTUtils.h>
#import <React/UIView+React.h>

@implementation NVNavigationStackView
{
    __weak RCTBridge *_bridge;
    NSMutableDictionary *_scenes;
    NSInteger _nativeEventCount;
    BOOL _navigated;
}

- (id)initWithBridge:(RCTBridge *)bridge
{
    if (self = [super init]) {
        _bridge = bridge;
        _navigationController = [[NVStackController alloc] init];
        _navigationController.view.semanticContentAttribute = ![[RCTI18nUtil sharedInstance] isRTL] ? UISemanticContentAttributeForceLeftToRight : UISemanticContentAttributeForceRightToLeft;
        _navigationController.navigationBar.semanticContentAttribute = ![[RCTI18nUtil sharedInstance] isRTL] ? UISemanticContentAttributeForceLeftToRight : UISemanticContentAttributeForceRightToLeft;
        [self addSubview:_navigationController.view];
        _navigationController.delegate = self;
        _scenes = [[NSMutableDictionary alloc] init];
    }
    return self;
}

- (void)insertReactSubview:(UIView *)subview atIndex:(NSInteger)atIndex
{
    [super insertReactSubview:subview atIndex:atIndex];
    _scenes[((NVSceneView *) subview).sceneKey] = subview;
}

- (void)removeReactSubview:(UIView *)subview
{
    [super removeReactSubview:subview];
    [_scenes removeObjectForKey:((NVSceneView *) subview).sceneKey];
}

- (void)didUpdateReactSubviews
{
}

- (void)didSetProps:(NSArray<NSString *> *)changedProps
{
    [super didSetProps:changedProps];
    if (!_navigated) {
        [self navigate];
        _navigated = YES;
    } else {
        dispatch_async(dispatch_get_main_queue(), ^{
            [self navigate];
        });
    }
}

- (void)navigate
{
    NSInteger eventLag = _nativeEventCount - _mostRecentEventCount;
    if (eventLag != 0 || _scenes.count == 0)
        return;
    NSInteger crumb = [self.keys count] - 1;
    NSInteger currentCrumb = [_navigationController.viewControllers count] - 1;
    if (crumb < currentCrumb) {
        [_navigationController popToViewController:_navigationController.viewControllers[crumb] animated:true];
    }
    BOOL animate = ![self.enterAnim isEqualToString:@""];
    if (crumb > currentCrumb) {
        NSMutableArray *controllers = [[NSMutableArray alloc] init];
        for(NSInteger i = 0; i < crumb - currentCrumb; i++) {
            NSInteger nextCrumb = currentCrumb + i + 1;
            NVSceneView *scene = (NVSceneView *) [_scenes objectForKey:[self.keys objectAtIndex:nextCrumb]];
            if (!![scene superview])
                return;
            NVSceneController *controller = [[NVSceneController alloc] initWithScene:scene];
            __weak typeof(self) weakSelf = self;
            controller.boundsDidChangeBlock = ^(NVSceneController *sceneController) {
                [weakSelf notifyForBoundsChange:sceneController];
            };
            scene.peekableDidChangeBlock = ^{
                [weakSelf checkPeekability:[self.keys count] - 1];
            };
            controller.navigationItem.title = scene.title;
            [controllers addObject:controller];
        }
        __block BOOL completed = NO;
        [self completeNavigation:^{
            if (completed) return;
            completed = YES;
            if (crumb - currentCrumb == 1) {
                [self->_navigationController pushViewController:controllers[0] animated:animate];
            } else {
                NSArray *allControllers = [self->_navigationController.viewControllers arrayByAddingObjectsFromArray:controllers];
                [self->_navigationController setViewControllers:allControllers animated:animate];
            }
        } waitOn:((UIViewController *) [controllers lastObject]).view];
    }
    if (crumb == currentCrumb) {
        NVSceneView *scene = (NVSceneView *) [_scenes objectForKey:[self.keys objectAtIndex:crumb]];
        if (!![scene superview])
            return;
        NVSceneController *controller = [[NVSceneController alloc] initWithScene:scene];
        NSMutableArray *controllers = [NSMutableArray arrayWithArray:_navigationController.viewControllers];
        [controllers replaceObjectAtIndex:crumb withObject:controller];
        __block BOOL completed = NO;
        [self completeNavigation:^{
            if (completed) return;
            completed = YES;
            [self->_navigationController setViewControllers:controllers animated:animate];
        } waitOn:scene];
    }
}

-(void) completeNavigation:(void (^)(void)) completeNavigation waitOn:(UIView *)scene
{
    NVNavigationBarView *navigationBar = [self findNavigationBar:scene];
    if (!navigationBar.backImageLoading) {
        completeNavigation();
    } else {
        navigationBar.backImageDidLoadBlock = completeNavigation;
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, .1 * NSEC_PER_SEC), dispatch_get_main_queue(), completeNavigation);
    }
}

-(NVNavigationBarView *) findNavigationBar:(UIView *)parent
{
    for(NSInteger i = 0; i < parent.subviews.count; i++) {
        UIView* subview = parent.subviews[i];
        if ([subview isKindOfClass:[NVNavigationBarView class]])
            return (NVNavigationBarView *) subview;
        subview = [self findNavigationBar:parent.subviews[i]];
        if ([subview isKindOfClass:[NVNavigationBarView class]])
            return (NVNavigationBarView *) subview;
    }
    return nil;
}

- (void)didMoveToWindow
{
    [super didMoveToWindow];
    UIView *parentView = (UIView *)self.superview;
    while (!self.navigationController.parentViewController && parentView) {
        if (parentView.reactViewController) {
            [parentView.reactViewController addChildViewController:self.navigationController];
        }
        parentView = parentView.superview;
    }
}

- (void)notifyForBoundsChange:(NVSceneController *)controller
{
    [_bridge.uiManager setSize:controller.view.bounds.size forView:controller.view];
}

- (void)checkPeekability:(NSInteger)crumb
{
    NVSceneView *scene;
    if (crumb > 1 && self.keys.count > crumb - 1) {
        scene = (NVSceneView *) [_scenes objectForKey:[self.keys objectAtIndex:crumb - 1]];
    }
    _navigationController.interactivePopGestureRecognizer.enabled = scene ? scene.subviews.count > 0 : YES;
}

- (void)layoutSubviews
{
    [super layoutSubviews];
    _navigationController.view.frame = self.bounds;
}

- (void)dealloc
{
    _navigationController.delegate = nil;
}

- (void)navigationController:(UINavigationController *)navigationController willShowViewController:(UIViewController *)viewController animated:(BOOL)animated
{
    NSInteger crumb = [((NVSceneView *) viewController.view).crumb intValue];
    if (crumb < [self.keys count] - 1) {
        self.onWillNavigateBack(@{ @"crumb": @(crumb) });
    }
}

- (void)navigationController:(UINavigationController *)navigationController didShowViewController:(UIViewController *)viewController animated:(BOOL)animated
{
    NSInteger crumb = [navigationController.viewControllers indexOfObject:viewController];
    [self checkPeekability:crumb];
    if (crumb < [self.keys count] - 1) {
        _nativeEventCount++;
    }
    self.onRest(@{
        @"crumb": @(crumb),
        @"eventCount": crumb < [self.keys count] - 1 ? @(_nativeEventCount) : @0
    });
}

@end

@implementation NVStackController

- (UIViewController *)childViewControllerForStatusBarStyle
{
    return self.visibleViewController;
}

- (UIViewController *)childViewControllerForStatusBarHidden
{
    return self.visibleViewController;
}

- (BOOL)navigationBar:(UINavigationBar *)navigationBar shouldPopItem:(UINavigationItem *)item
{
    NVSceneView *scene;
    NSInteger crumb = [[navigationBar items] indexOfObject:item];
    if (self.viewControllers.count > crumb - 1)
        scene = ((NVSceneView *) [self.viewControllers objectAtIndex:crumb - 1].view);
    return scene ? scene.subviews.count > 0 : YES;
}

@end

@implementation UIViewController (Navigation)

- (UIViewController *)navigation_childViewControllerForStatusBarStyle
{
    return [self childViewControllerForStatusBar];
}

- (UIViewController *)navigation_childViewControllerForStatusBarHidden
{
    return [self childViewControllerForStatusBar];
}

- (UIViewController *)childViewControllerForStatusBar
{
    UIViewController *viewController = [[self childViewControllers] lastObject];
    return ([viewController isKindOfClass:[UINavigationController class]] || [viewController isKindOfClass:[UITabBarController class]]) ? viewController : nil;
}

+ (void)load
{
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        RCTSwapInstanceMethods([UIViewController class], @selector(childViewControllerForStatusBarStyle), @selector(navigation_childViewControllerForStatusBarStyle));
        RCTSwapInstanceMethods([UIViewController class], @selector(childViewControllerForStatusBarHidden), @selector(navigation_childViewControllerForStatusBarHidden));
    });
}

@end
