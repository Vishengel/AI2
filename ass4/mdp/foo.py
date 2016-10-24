import mdp
import sys

"""
if len(sys.argv) != 2:
    print "Please enter stop_crit and gamma as arguments"
    exit(1)
"""

m1 = mdp.makeRNProblem()
m1.valueIteration()
#m1.policyIteration()
m1.printValues()
m1.printActions()

m2 = mdp.make2DProblem()
m2.valueIteration()
#m2.policyIteration()
m2.printValues()
m2.printActions()